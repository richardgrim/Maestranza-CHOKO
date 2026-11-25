package bo.choko.clientes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ClienteRepositoryTest {
    @Autowired ClienteRepository clientes;

    @Test
    void registraYEncuentraPorParteDelNombre() {
        Cliente nuevo = clientes.saveAndFlush(new Cliente("Taller Ficticio CHOKO", null));
        assertThat(clientes.findByNombreContainingIgnoreCaseOrderByNombreAscIdAsc(
                "ficticio", PageRequest.of(0, 20))).extracting(Cliente::getId).contains(nuevo.getId());
    }
}
