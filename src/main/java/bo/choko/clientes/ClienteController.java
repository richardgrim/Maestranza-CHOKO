package bo.choko.clientes;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClienteController {
    private final ClienteService clientes;

    public ClienteController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/clientes";
    }

    @GetMapping("/clientes")
    public String listado(@RequestParam(defaultValue = "") String q,
                          @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("clientes", clientes.buscar(q, pagina));
        return "clientes";
    }

    @GetMapping("/clientes/nuevo")
    public String formulario() {
        return "nuevo-cliente";
    }

    @PostMapping("/clientes")
    public String registrar(@RequestParam String nombre, @RequestParam(defaultValue = "") String telefono,
                            Model model, RedirectAttributes flash) {
        try {
            clientes.registrar(nombre, telefono);
        } catch (IllegalArgumentException error) {
            model.addAttribute("error", error.getMessage());
            model.addAttribute("nombre", nombre);
            model.addAttribute("telefono", telefono);
            return "nuevo-cliente";
        }
        flash.addFlashAttribute("aviso", "Cliente registrado.");
        return "redirect:/clientes";
    }
}
