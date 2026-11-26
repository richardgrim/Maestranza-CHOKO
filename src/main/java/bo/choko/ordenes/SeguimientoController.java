package bo.choko.ordenes;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ordenes/{id}/seguimiento")
public class SeguimientoController {
    private final SeguimientoService servicio;
    public SeguimientoController(SeguimientoService servicio) { this.servicio = servicio; }

    @GetMapping
    public String ver(@PathVariable long id, Model model, HttpServletRequest request) {
        model.addAttribute("orden", servicio.orden(id));
        model.addAttribute("historial", servicio.historial(id));
        model.addAttribute("operador", request.isUserInRole("OPERADOR"));
        model.addAttribute("tokenEstado", UUID.randomUUID());
        model.addAttribute("tokenRepuesto", UUID.randomUUID());
        model.addAttribute("tokenAviso", UUID.randomUUID());
        return "seguimiento";
    }

    @PostMapping("/repuestos")
    @PreAuthorize("hasRole('OPERADOR')")
    public String repuesto(@PathVariable long id, @RequestParam UUID solicitud, @RequestParam String nombre,
                           @RequestParam int cantidad, Principal usuario) {
        servicio.repuesto(id, solicitud, nombre, cantidad, usuario.getName());
        return volver(id);
    }

    @PostMapping("/estado")
    @PreAuthorize("hasRole('OPERADOR')")
    public String estado(@PathVariable long id, @RequestParam UUID solicitud,
                         @RequestParam SeguimientoService.Estado estado, Principal usuario) {
        servicio.cambiar(id, solicitud, estado, usuario.getName());
        return volver(id);
    }

    @PostMapping("/avisos")
    @PreAuthorize("hasRole('OPERADOR')")
    public String aviso(@PathVariable long id, @RequestParam UUID solicitud, @RequestParam String medio,
                        @RequestParam LocalDate fecha, @RequestParam String nota, Principal usuario) {
        servicio.aviso(id, solicitud, medio, fecha, nota, usuario.getName());
        return volver(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String invalido(IllegalArgumentException error, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error", error.getMessage());
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return "redirect:" + ruta.substring(0, ruta.lastIndexOf('/'));
    }

    private String volver(long id) { return "redirect:/ordenes/" + id + "/seguimiento"; }
}
