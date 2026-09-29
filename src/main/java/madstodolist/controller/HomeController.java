package madstodolist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import madstodolist.authentication.ManagerUserSession;
import madstodolist.service.UsuarioService;
import madstodolist.dto.UsuarioData;

@Controller
public class HomeController {

    @Autowired
    private ManagerUserSession managerUserSession;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/about")
    public String about(Model model) {
        Long idUsuario = managerUserSession.usuarioLogeado();
        if (idUsuario != null) {
            UsuarioData usuario = usuarioService.findById(idUsuario);
            model.addAttribute("usuario", usuario);
        }
        return "about";
    }
}