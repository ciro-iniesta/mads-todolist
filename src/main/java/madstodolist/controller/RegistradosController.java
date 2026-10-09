package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import madstodolist.authentication.ManagerUserSession;

import java.util.List;

@Controller
public class RegistradosController {

    @Autowired
    UsuarioService usuarioService;
    @Autowired
    private ManagerUserSession managerUserSession;  

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {
        cargarUsuarioActual(model);

        // Obtenemos la lista de usuarios a través del servicio
        List<UsuarioData> usuarios = usuarioService.allUsuarios();

        // Pasamos la lista a la vista mediante el modelo
        model.addAttribute("usuarios", usuarios);

        // Devolvemos el nombre de la plantilla HTML
        return "registrados";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable("id") Long id, Model model) {
        cargarUsuarioActual(model);

        // Recuperamos el DTO de la capa de servicio
        UsuarioData usuario = usuarioService.findById(id);

        if (usuario == null) {
            return "redirect:/registrados"; // Seguridad adicional si el ID no existe
        }

        // LIMPIEZA DE DATOS SENSIBLES ANTES DE LA VISTA
        usuario.setPassword(null);

        model.addAttribute("usuarioDetalle", usuario);
        return "usuarioDesc";
    }

    private void cargarUsuarioActual(Model model) {
        Long usuarioId = managerUserSession.usuarioLogeado();
        if (usuarioId != null) {
            UsuarioData usuarioActual = usuarioService.findById(usuarioId);
            model.addAttribute("usuario", usuarioActual);
        }
    }
}