package pa.conectapty.app.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star as EstrellaVacia
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.filtrarEstaciones

@Composable
fun SelectorEstacion(
    titulo: String,
    estaciones: List<Estacion>,
    favoritos: List<Estacion>,
    onElegir: (Estacion) -> Unit,
    onAlternarFavorito: (Estacion) -> Unit,
    modifier: Modifier = Modifier,
) {
    var consulta by rememberSaveable { mutableStateOf("") }
    // Solo se vuelve a filtrar cuando cambia el texto o el catálogo, no en cada recomposición
    val visibles = remember(consulta, estaciones) { filtrarEstaciones(estaciones, consulta) }
    val idsFavoritos = remember(favoritos) { favoritos.mapTo(HashSet()) { it.id } }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = consulta,
            onValueChange = { consulta = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Busca una estación") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        if (estaciones.isEmpty()) {
            Text("Todavía no hay estaciones descargadas.", Modifier.padding(16.dp))
        } else if (visibles.isEmpty()) {
            Text("Ninguna estación coincide con \"$consulta\".", Modifier.padding(16.dp))
        }
        LazyColumn {
            items(visibles, key = { it.id }) { e ->
                val esFav = e.id in idsFavoritos
                ListItem(
                    headlineContent = { Text(e.nombre) },
                    trailingContent = {
                        IconButton(onClick = { onAlternarFavorito(e) }) {
                            Icon(
                                if (esFav) Icons.Filled.Star else Icons.Outlined.EstrellaVacia,
                                contentDescription = if (esFav) "Quitar de favoritos" else "Guardar en favoritos",
                            )
                        }
                    },
                    modifier = Modifier.clickable { onElegir(e) },
                )
                HorizontalDivider()
            }
        }
    }
}
