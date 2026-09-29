package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AcercaDeWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManagerUserSession managerUserSession;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void getAboutDevuelveNombreAplicacionYAutor() throws Exception {
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        this.mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ToDoList")))
                .andExpect(content().string(containsString("Ciro Iniesta Formisano")))
                .andExpect(content().string(containsString("1.0.1-SNAPSHOT")));
    }

    @Test
    public void getAboutMuestraNavbarLogeado() throws Exception {
        UsuarioData usuario = new UsuarioData();
        usuario.setId(1L);
        usuario.setNombre("Ciro");

        when(managerUserSession.usuarioLogeado()).thenReturn(1L);
        when(usuarioService.findById(1L)).thenReturn(usuario);

        this.mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tareas")))
                .andExpect(content().string(containsString("Cerrar sesión Ciro")));
    }

    @Test
    public void getAboutMuestraNavbarNoLogeado() throws Exception {
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        this.mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login")))
                .andExpect(content().string(containsString("Registro")));
    }
}