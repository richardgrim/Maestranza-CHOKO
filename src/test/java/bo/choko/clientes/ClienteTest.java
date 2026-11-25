package bo.choko.clientes;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ClienteTest {
    @Test
    void exigeNombre() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Cliente("   ", null));
    }

    @Test
    void conservaPrefijoTelefonicoYQuitaEspacios() {
        Cliente cliente = new Cliente("  Cliente de prueba ", " +591 70000000 ");
        assertThat(cliente.getNombre()).isEqualTo("Cliente de prueba");
        assertThat(cliente.getTelefono()).isEqualTo("+591 70000000");
    }
}
