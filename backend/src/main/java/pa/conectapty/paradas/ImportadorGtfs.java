package pa.conectapty.paradas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import pa.conectapty.comun.ConectaProperties;

/**
 * Al arrancar, carga las estaciones del GTFS generado por data-platform.
 * Es idempotente: si el archivo (por su SHA-256) ya se importó, no hace nada.
 * Todo ocurre en una transacción: o se publica completo o no se publica.
 */
@Component
@Order(10) // antes que el calentador de OTP, que usa estas estaciones
public class ImportadorGtfs implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportadorGtfs.class);

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final ConectaProperties props;

    public ImportadorGtfs(JdbcClient jdbc, TransactionTemplate tx, ConectaProperties props) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path zip = Path.of(props.gtfs().path());
        if (!Files.exists(zip)) {
            log.warn("No se encontró el GTFS en {}. Genera uno con data-platform. La API arranca sin estaciones.",
                    zip.toAbsolutePath());
            return;
        }
        String sha = sha256(zip);
        boolean yaImportado = jdbc.sql("SELECT count(*) FROM importacion_datos WHERE sha256 = :s AND estado = 'publicada'")
                .param("s", sha).query(Long.class).single() > 0;
        if (yaImportado) {
            log.info("GTFS {} ya importado; sin cambios.", zip.getFileName());
            return;
        }
        List<Map<String, String>> stops = leerCsv(zip, "stops.txt");
        String version = versionFeed(zip);
        tx.executeWithoutResult(estado -> publicar(zip, sha, version, stops));
        log.info("Importadas {} paradas desde {} (versión {})", stops.size(), zip.getFileName(), version);
    }

    /** feed_info.feed_version (fecha de generación en data-platform), o null si no viene. */
    static String versionFeed(Path zip) {
        try {
            List<Map<String, String>> info = leerCsv(zip, "feed_info.txt");
            String v = info.isEmpty() ? null : info.get(0).get("feed_version");
            return v == null || v.isBlank() ? null : v;
        } catch (IOException e) {
            return null;
        }
    }

    private void publicar(Path zip, String sha, String version, List<Map<String, String>> stops) {
        long fuenteId = jdbc.sql("SELECT id FROM fuente_datos WHERE nombre = 'OpenStreetMap'")
                .query(Long.class).single();
        long importacionId = jdbc.sql("""
                INSERT INTO importacion_datos (fuente_id, archivo, sha256, registros, estado, version_feed)
                VALUES (:f, :a, :s, :n, 'publicada', :v) RETURNING id
                """)
                .param("f", fuenteId).param("a", zip.getFileName().toString())
                .param("s", sha).param("n", stops.size()).param("v", version)
                .query(Long.class).single();

        // Versión anterior queda registrada en importacion_datos; las paradas se reemplazan completas.
        jdbc.sql("UPDATE importacion_datos SET estado = 'revertida' WHERE estado = 'publicada' AND id <> :id")
                .param("id", importacionId).update();
        jdbc.sql("DELETE FROM parada").update();

        for (Map<String, String> s : stops) {
            String padre = s.getOrDefault("parent_station", "");
            jdbc.sql("""
                    INSERT INTO parada (id, nombre, ubicacion, tipo, estacion_padre, accesible, importacion_id)
                    VALUES (:id, :nombre, ST_MakePoint(:lon, :lat)::geography, :tipo, :padre, :acc, :imp)
                    """)
                    .param("id", s.get("stop_id"))
                    .param("nombre", s.get("stop_name"))
                    .param("lat", Double.parseDouble(s.get("stop_lat")))
                    .param("lon", Double.parseDouble(s.get("stop_lon")))
                    .param("tipo", Short.parseShort(s.getOrDefault("location_type", "0").isBlank() ? "0" : s.get("location_type")))
                    .param("padre", padre.isBlank() ? null : padre)
                    .param("acc", Short.parseShort(s.getOrDefault("wheelchair_boarding", "0").isBlank() ? "0" : s.get("wheelchair_boarding")))
                    .param("imp", importacionId)
                    .update();
        }
    }

    /** Lector CSV mínimo para GTFS (admite comillas dobles y BOM). */
    static List<Map<String, String>> leerCsv(Path zip, String archivo) throws IOException {
        try (ZipFile z = new ZipFile(zip.toFile())) {
            ZipEntry e = z.getEntry(archivo);
            if (e == null) {
                throw new IOException(archivo + " no existe dentro de " + zip);
            }
            try (InputStream in = z.getInputStream(e);
                 BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String cabecera = br.readLine();
                if (cabecera == null) return List.of();
                if (cabecera.startsWith("﻿")) cabecera = cabecera.substring(1);
                List<String> columnas = dividir(cabecera);
                List<Map<String, String>> filas = new ArrayList<>();
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.isBlank()) continue;
                    List<String> valores = dividir(linea);
                    Map<String, String> fila = new HashMap<>();
                    for (int i = 0; i < columnas.size(); i++) {
                        fila.put(columnas.get(i).trim(), i < valores.size() ? valores.get(i) : "");
                    }
                    filas.add(fila);
                }
                return filas;
            }
        }
    }

    static List<String> dividir(String linea) {
        List<String> out = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == ',' && !entreComillas) {
                out.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        out.add(actual.toString());
        return out;
    }

    private static String sha256(Path archivo) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(archivo)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        return HexFormat.of().formatHex(md.digest());
    }
}
