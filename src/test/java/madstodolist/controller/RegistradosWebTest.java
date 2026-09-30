package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RegistradosWebTest {

    @Autowired
    private MockMvc mockMvc;

    // Moqueamos el servicio siguiendo el enfoque de MADS
    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void listadoUsuariosRegistrados() throws Exception {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setEmail("test.registro@ua.es");

        // Cuando el controller llame a allUsuarios(), le devolvemos esta lista ficticia
        when(usuarioService.allUsuarios()).thenReturn(Arrays.asList(usuario));

        // WHEN, THEN
        // Hacemos un GET a la ruta y verificamos el estado OK y el contenido HTML
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("test.registro@ua.es")));
    }
}