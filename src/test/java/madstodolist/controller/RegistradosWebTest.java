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
import static org.hamcrest.Matchers.not;
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

    @Test
    public void descripcionUsuarioMuestraDatosSinPassword() throws Exception {
        // GIVEN: Un usuario en la base de datos (simulado)
        UsuarioData usuario = new UsuarioData();
        usuario.setId(1L);
        usuario.setEmail("ciro@ua.es");
        usuario.setNombre("Ciro Iniesta");
        usuario.setPassword("supersecreta123"); // Contraseña que NO debe viajar a la vista

        // Mockeamos el servicio que ya tienes implementado
        when(usuarioService.findById(1L)).thenReturn(usuario);

        // WHEN + THEN: Hacemos la petición y validamos las COS
        this.mockMvc.perform(get("/registrados/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ciro@ua.es")))
                .andExpect(content().string(containsString("Ciro Iniesta")))
                // VALIDACIÓN DE SEGURIDAD: La contraseña no se expone en absoluto
                .andExpect(content().string(not(containsString("supersecreta123"))));
    }
}