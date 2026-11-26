package bo.choko.ordenes;

import bo.choko.clientes.Cliente;
import bo.choko.clientes.ClienteRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguimientoTest {
    @Autowired ClienteRepository clientes;
    @Autowired OrdenService ordenes;
    @Autowired SeguimientoService servicio;
    @Autowired MockMvc mvc;

    long orden() {
        long id = clientes.saveAndFlush(new Cliente("Taller ficticio", null)).getId();
        LocalDate dia = LocalDate.of(2025, 1, 2);
        ordenes.registrar(id, "Culata", "Rectificación", dia, dia.plusDays(1));
        return ordenes.listar(id).getFirst().id();
    }

    @Test
    void historialIdentificaOrdenUsuarioYReintentos() {
        long id = orden(), otra = orden();
        UUID solicitud = UUID.randomUUID();
        servicio.repuesto(id, solicitud, "Guías", 4, "ana");
        servicio.repuesto(id, solicitud, "Guías", 4, "ana");
        servicio.cambiar(id, UUID.randomUUID(), SeguimientoService.Estado.RECIBIDA, "ana");
        servicio.aviso(id, UUID.randomUUID(), "WhatsApp", LocalDate.of(2025,1,3), "Se informó del retraso", "luis");
        var eventos = servicio.historial(id);
        assertThat(eventos).extracting(SeguimientoService.Evento::tipo).containsExactly("AVISO","ESTADO","REPUESTO");
        assertThat(eventos.getFirst().usuario()).isEqualTo("luis");
        assertThat(eventos.getLast().cantidad()).isEqualTo(4);
        assertThat(servicio.historial(otra)).isEmpty();
    }

    @Test
    void impideSaltarEstados() {
        long id = orden();
        assertThatThrownBy(() -> servicio.cambiar(id, UUID.randomUUID(), SeguimientoService.Estado.ENTREGADA, "op"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(servicio.orden(id).estado()).isEqualTo(SeguimientoService.Estado.SIN_REGISTRO);
        assertThat(servicio.historial(id)).isEmpty();
    }

    @Test
    void lectorPuedeConsultarPeroNoModificar() throws Exception {
        String ruta = "/ordenes/" + orden() + "/seguimiento";
        mvc.perform(get(ruta).with(user("lector"))).andExpect(status().isOk());
        for (String accion : new String[]{"repuestos","avisos","estado"}) {
            mvc.perform(post(ruta + "/" + accion).with(user("lector")).with(csrf())
                    .param("solicitud", UUID.randomUUID().toString()).param("nombre","Guías").param("cantidad","3")
                    .param("medio","Llamada").param("fecha","2025-01-03").param("nota","Aviso")
                    .param("estado","RECIBIDA")).andExpect(status().isForbidden());
        }
    }

    @Test
    void rechazaCantidadInvalida() throws Exception {
        long id = orden();
        String ruta = "/ordenes/" + id + "/seguimiento";
        mvc.perform(post(ruta + "/repuestos").with(user("op").roles("OPERADOR")).with(csrf())
                .param("solicitud", UUID.randomUUID().toString()).param("nombre","Guías").param("cantidad","0"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("error", containsString("positiva")));
        assertThat(servicio.historial(id)).isEmpty();
    }
}
