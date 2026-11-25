package bo.choko.ordenes;

import bo.choko.clientes.Cliente;
import bo.choko.clientes.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrdenWebTest {
    @Autowired MockMvc mvc;
    @Autowired ClienteRepository clientes;
    @Autowired OrdenService ordenes;

    private long cliente(String nombre) {
        return clientes.saveAndFlush(new Cliente(nombre, null)).getId();
    }

    private String ruta(long id) {
        return "/clientes/" + id + "/ordenes";
    }

    private MockHttpServletRequestBuilder alta(long id, String rol, String componente, String entrega) {
        return post(ruta(id)).with(user("personal").roles(rol)).with(csrf())
                .param("componente", componente).param("servicios", "Rectificar y verificar planitud")
                .param("recepcion", "2026-11-25").param("entrega", entrega);
    }

    @Test
    void registraYConsultaLaOrdenDelClienteCorrecto() throws Exception {
        long id = cliente("Cliente de prueba");
        long otro = cliente("Segundo cliente");
        mvc.perform(get(ruta(id) + "/nueva").with(user("op").roles("OPERADOR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Cliente de prueba")));
        mvc.perform(alta(id, "OPERADOR", "Culata", "2026-11-25")).andExpect(redirectedUrl(ruta(id)));
        mvc.perform(get(ruta(id)).with(user("op").roles("OPERADOR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Culata")))
                .andExpect(content().string(containsString("Rectificar y verificar planitud")))
                .andExpect(content().string(containsString("2026-11-25")));
        assertThat(ordenes.listar(otro)).isEmpty();
    }

    @Test
    void rechazaComponenteVacioYEntregaAnterior() throws Exception {
        long id = cliente("Prueba de validación");
        mvc.perform(alta(id, "OPERADOR", "  ", "2026-11-25")).andExpect(view().name("nueva-orden"))
                .andExpect(model().attributeExists("error"));
        mvc.perform(alta(id, "OPERADOR", "Culata", "2026-11-24")).andExpect(view().name("nueva-orden"))
                .andExpect(model().attributeExists("error"));
        assertThat(ordenes.listar(id)).isEmpty();
    }

    @Test
    void deniegaElAltaSinRolYConservaLosRegistros() throws Exception {
        long id = cliente("Prueba de acceso");
        mvc.perform(alta(id, "LECTOR", "Culata", "2026-11-26")).andExpect(status().isForbidden());
        mvc.perform(get(ruta(id)).with(user("lector").roles("LECTOR"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Registrar orden"))));
        assertThat(ordenes.listar(id)).isEmpty();
    }

    @Test
    void rechazaOrdenParaClienteInexistente() throws Exception {
        mvc.perform(alta(-1, "OPERADOR", "Culata", "2026-11-26")).andExpect(status().isNotFound());
    }
}
