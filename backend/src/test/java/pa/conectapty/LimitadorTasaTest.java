package pa.conectapty;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import pa.conectapty.comun.LimitadorTasa;

class LimitadorTasaTest {

    @Test
    void bloqueaAlPasarElLimiteYReabreElMinutoSiguiente() {
        AtomicLong reloj = new AtomicLong(120_000);           // inicio exacto de un minuto
        LimitadorTasa l = new LimitadorTasa(3, reloj::get);
        assertThat(l.intentar("1.1.1.1")).isZero();
        assertThat(l.intentar("1.1.1.1")).isZero();
        assertThat(l.intentar("1.1.1.1")).isZero();
        reloj.addAndGet(15_000);
        assertThat(l.intentar("1.1.1.1")).isEqualTo(45);      // 45 s hasta el próximo minuto
        assertThat(l.intentar("2.2.2.2")).isZero();           // otra IP no se ve afectada
        reloj.addAndGet(45_000);
        assertThat(l.intentar("1.1.1.1")).isZero();
    }
}
