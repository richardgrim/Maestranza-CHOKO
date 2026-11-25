package bo.choko.clientes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.security.user.name=operador", "spring.security.user.password=prueba-local"})
@AutoConfigureMockMvc
@Transactional
class ClienteWebTest {
    @Autowired MockMvc mvc;

    @Test
    void exigeSesionParaConsultarClientes() throws Exception {
        mvc.perform(get("/clientes").accept("text/html")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void permiteIniciarSesionConLaCuentaConfigurada() throws Exception {
        mvc.perform(formLogin().user("operador").password("prueba-local")).andExpect(authenticated());
    }

    @Test
    void exigeTokenCsrfParaRegistrar() throws Exception {
        mvc.perform(post("/clientes").with(user("operador")).param("nombre", "Sin token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registraYBuscaDesdeLasVistas() throws Exception {
        mvc.perform(get("/clientes/nuevo").with(user("operador")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("_csrf")));
        mvc.perform(post("/clientes").with(user("operador")).with(csrf())
                        .param("nombre", "Taller Demostración").param("telefono", "+591 70000000"))
                .andExpect(redirectedUrl("/clientes"));
        mvc.perform(get("/clientes").with(user("operador")).param("q", "demostración"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Taller Demostración")))
                .andExpect(content().string(containsString("+591 70000000")));
    }

    @Test
    void muestraElErrorSiElNombreEstaVacio() throws Exception {
        mvc.perform(post("/clientes").with(user("operador")).with(csrf()).param("nombre", " "))
                .andExpect(status().isOk()).andExpect(view().name("nuevo-cliente"))
                .andExpect(content().string(containsString("El nombre debe tener")));
    }
}
