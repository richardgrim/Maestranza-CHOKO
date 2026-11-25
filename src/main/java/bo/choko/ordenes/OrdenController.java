package bo.choko.ordenes;

import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes/{id}/ordenes")
public class OrdenController {
    private final OrdenService ordenes;

    public OrdenController(OrdenService ordenes) {
        this.ordenes = ordenes;
    }

    @GetMapping
    public String lista(@PathVariable long id, jakarta.servlet.http.HttpServletRequest solicitud, Model model) {
        model.addAttribute("cliente", ordenes.cliente(id));
        model.addAttribute("puedeRegistrar", solicitud.isUserInRole("OPERADOR"));
        model.addAttribute("ordenes", ordenes.listar(id));
        return "ordenes";
    }

    @GetMapping("/nueva")
    @PreAuthorize("hasRole('OPERADOR')")
    public String nueva(@PathVariable long id, Model model) {
        model.addAttribute("cliente", ordenes.cliente(id));
        return "nueva-orden";
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public String guardar(@PathVariable long id, @RequestParam(defaultValue = "") String componente,
                          @RequestParam(defaultValue = "") String servicios,
                          @RequestParam(required = false) LocalDate recepcion,
                          @RequestParam(required = false) LocalDate entrega, Model model, RedirectAttributes flash) {
        try {
            ordenes.registrar(id, componente, servicios, recepcion, entrega);
        } catch (IllegalArgumentException error) {
            model.addAttribute("cliente", ordenes.cliente(id));
            model.addAttribute("componente", componente);
            model.addAttribute("servicios", servicios);
            model.addAttribute("recepcion", recepcion);
            model.addAttribute("entrega", entrega);
            model.addAttribute("error", error.getMessage());
            return "nueva-orden";
        }
        flash.addFlashAttribute("aviso", "Orden registrada.");
        return "redirect:/clientes/" + id + "/ordenes";
    }
}
