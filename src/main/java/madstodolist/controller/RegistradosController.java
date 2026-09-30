package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class RegistradosController {

    @Autowired
    UsuarioService usuarioService;

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {
        // Obtenemos la lista de usuarios a través del servicio
        List<UsuarioData> usuarios = usuarioService.allUsuarios();

        // Pasamos la lista a la vista mediante el modelo
        model.addAttribute("usuarios", usuarios);

        // Devolvemos el nombre de la plantilla HTML
        return "registrados";
    }
}