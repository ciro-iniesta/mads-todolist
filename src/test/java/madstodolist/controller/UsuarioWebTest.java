package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import madstodolist.service.UsuarioServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
//
// A diferencia de los tests web de tarea, donde usábamos los datos
// de prueba de la base de datos, aquí vamos a practicar otro enfoque:
// moquear el usuarioService.
public class UsuarioWebTest {

    @Autowired
    private MockMvc mockMvc;

    // Moqueamos el usuarioService.
    // En los tests deberemos proporcionar el valor devuelto por las llamadas
    // a los métodos de usuarioService que se van a ejecutar cuando se realicen
    // las peticiones a los endpoint.
    @MockBean
    private UsuarioService usuarioService;

    @Test
    public void servicioLoginUsuarioOK() throws Exception {
        // GIVEN
        // Moqueamos la llamada a usuarioService.login para que
        // devuelva un LOGIN_OK y la llamada a usuarioServicie.findByEmail
        // para que devuelva un usuario determinado.

        UsuarioData anaGarcia = new UsuarioData();
        anaGarcia.setNombre("Ana García");
        anaGarcia.setId(1L);

        when(usuarioService.login("ana.garcia@gmail.com", "12345678"))
                .thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
        when(usuarioService.findByEmail("ana.garcia@gmail.com"))
                .thenReturn(anaGarcia);

        // WHEN, THEN
        // Realizamos una petición POST al login pasando los datos
        // esperados en el mock, la petición devolverá una redirección a la
        // URL con las tareas del usuario

        this.mockMvc.perform(post("/login")
                        .param("eMail", "ana.garcia@gmail.com")
                        .param("password", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/1/tareas"))
                .andExpect(request().sessionAttribute(
                "idUsuarioLogeado", 1L))
                .andExpect(request().sessionAttribute(
                "usuarioAdministrador", false));
    }

    @Test
    public void servicioLoginUsuarioNotFound() throws Exception {
        // GIVEN
        // Moqueamos el método usuarioService.login para que devuelva
        // USER_NOT_FOUND
        when(usuarioService.login("pepito.perez@gmail.com", "12345678"))
                .thenReturn(UsuarioService.LoginStatus.USER_NOT_FOUND);

        // WHEN, THEN
        // Realizamos una petición POST con los datos del usuario mockeado y
        // se debe devolver una página que contenga el mensaja "No existe usuario"
        this.mockMvc.perform(post("/login")
                        .param("eMail","pepito.perez@gmail.com")
                        .param("password","12345678"))
                .andExpect(content().string(containsString("No existe usuario")));
    }

    @Test
    public void servicioLoginUsuarioErrorPassword() throws Exception {
        // GIVEN
        // Moqueamos el método usuarioService.login para que devuelva
        // ERROR_PASSWORD
        when(usuarioService.login("ana.garcia@gmail.com", "000"))
                .thenReturn(UsuarioService.LoginStatus.ERROR_PASSWORD);

        // WHEN, THEN
        // Realizamos una petición POST con los datos del usuario mockeado y
        // se debe devolver una página que contenga el mensaja "Contraseña incorrecta"
        this.mockMvc.perform(post("/login")
                        .param("eMail","ana.garcia@gmail.com")
                        .param("password","000"))
                .andExpect(content().string(containsString("Contraseña incorrecta")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void registroMuestraCasillaSegunExistaAdministrador(
            boolean existeAdministrador) throws Exception {

        when(usuarioService.existeAdministrador()).thenReturn(existeAdministrador);

        ResultActions respuesta = mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("formRegistro"))
                .andExpect(model().attribute("mostrarAdministrador", !existeAdministrador));

        // El nombre exacto distingue la casilla del campo oculto _administrador.
        if (existeAdministrador) {
            respuesta.andExpect(content().string(not(containsString("name=\"administrador\""))));
        } else {
            respuesta.andExpect(content().string(containsString("name=\"administrador\"")))
                    .andExpect(content().string(containsString("type=\"checkbox\"")));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void registroTransmiteRolAlServicio(boolean administrador) throws Exception {
        MockHttpServletRequestBuilder peticion = post("/registro")
                .param("email", "nuevo@ua.es")
                .param("password", "12345678")
                .param("nombre", "Usuario nuevo");

        // Una casilla desmarcada no envía el parámetro administrador.
        if (administrador) {
            peticion.param("administrador", "true");
        }

        mockMvc.perform(peticion)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        ArgumentCaptor<UsuarioData> captor = ArgumentCaptor.forClass(UsuarioData.class);
        verify(usuarioService).registrar(captor.capture());

        UsuarioData enviado = captor.getValue();
        assertThat(enviado.getEmail()).isEqualTo("nuevo@ua.es");
        assertThat(enviado.getNombre()).isEqualTo("Usuario nuevo");
        assertThat(enviado.isAdministrador()).isEqualTo(administrador);
    }

    @Test
    public void loginAdministradorRedirigeAlListadoYGuardaSesion() throws Exception {
        UsuarioData administrador = new UsuarioData();
        administrador.setId(8L);
        administrador.setNombre("Administrador");
        administrador.setAdministrador(true);

        when(usuarioService.login("admin@ua.es", "12345678"))
                .thenReturn(UsuarioService.LoginStatus.LOGIN_OK);
        when(usuarioService.findByEmail("admin@ua.es")).thenReturn(administrador);

        mockMvc.perform(post("/login")
                        .param("eMail", "admin@ua.es")
                        .param("password", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"))
                .andExpect(request().sessionAttribute("idUsuarioLogeado", 8L))
                .andExpect(request().sessionAttribute("usuarioAdministrador", true));
    }

    @Test
    public void registroAdministradorRechazadoActualizaFormulario() throws Exception {
        // Simula un formulario abierto antes de que se registrara el administrador.
        when(usuarioService.registrar(any(UsuarioData.class)))
                .thenThrow(new UsuarioServiceException("Ya existe un usuario administrador"));
        when(usuarioService.existeAdministrador()).thenReturn(true);

        mockMvc.perform(post("/registro")
                        .param("email", "segundo@ua.es")
                        .param("password", "12345678")
                        .param("nombre", "Segundo administrador")
                        .param("administrador", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("formRegistro"))
                .andExpect(model().attribute("error", "Ya existe un usuario administrador"))
                .andExpect(model().attribute("mostrarAdministrador", false))
                .andExpect(model().attribute("registroData",
                        hasProperty("email", is("segundo@ua.es"))))
                .andExpect(content().string(containsString("Ya existe un usuario administrador")))
                .andExpect(content().string(not(containsString("name=\"administrador\""))));

        verify(usuarioService).registrar(any(UsuarioData.class));
    }

    @Test
    public void registroConFechaInvalidaConservaDatosYCasilla() throws Exception {
        when(usuarioService.existeAdministrador()).thenReturn(false);

        mockMvc.perform(post("/registro")
                        .param("email", "fecha@ua.es")
                        .param("password", "12345678")
                        .param("nombre", "Usuario con fecha invalida")
                        .param("fechaNacimiento", "fecha-invalida")
                        .param("administrador", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("formRegistro"))
                .andExpect(model().attributeHasFieldErrors("registroData", "fechaNacimiento"))
                .andExpect(model().attribute("registroData",
                        hasProperty("email", is("fecha@ua.es"))))
                .andExpect(model().attribute("registroData",
                        hasProperty("administrador", is(true))))
                .andExpect(model().attribute("mostrarAdministrador", true))
                .andExpect(content().string(containsString("name=\"administrador\"")))
                .andExpect(content().string(containsString("fecha-invalida")));

        verify(usuarioService, never()).registrar(any(UsuarioData.class));
    }

    @Test
    public void registroConEmailDuplicadoConservaDatosYActualizaCasilla() throws Exception {
        UsuarioData existente = new UsuarioData();
        existente.setEmail("repetido@ua.es");
        when(usuarioService.findByEmail("repetido@ua.es")).thenReturn(existente);
        when(usuarioService.existeAdministrador()).thenReturn(true);

        mockMvc.perform(post("/registro")
                        .param("email", "repetido@ua.es")
                        .param("password", "12345678")
                        .param("nombre", "Nombre conservado"))
                .andExpect(status().isOk())
                .andExpect(view().name("formRegistro"))
                .andExpect(model().attribute("error", "El usuario repetido@ua.es ya existe"))
                .andExpect(model().attribute("registroData",
                        hasProperty("email", is("repetido@ua.es"))))
                .andExpect(model().attribute("registroData",
                        hasProperty("nombre", is("Nombre conservado"))))
                .andExpect(model().attribute("mostrarAdministrador", false))
                .andExpect(content().string(containsString("El usuario repetido@ua.es ya existe")))
                .andExpect(content().string(not(containsString("name=\"administrador\""))));

        verify(usuarioService, never()).registrar(any(UsuarioData.class));
    }

    @Test
    public void registroConErrorDeIntegridadMuestraErrorSinDetallesInternos() throws Exception {
        String detalleInterno = "SQLSTATE 23505: restriccion interna de la base de datos";
        when(usuarioService.registrar(any(UsuarioData.class)))
                .thenThrow(new DataIntegrityViolationException(detalleInterno));
        when(usuarioService.existeAdministrador()).thenReturn(true);

        mockMvc.perform(post("/registro")
                        .param("email", "concurrente@ua.es")
                        .param("password", "12345678")
                        .param("administrador", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("formRegistro"))
                .andExpect(model().attribute("registroData",
                        hasProperty("email", is("concurrente@ua.es"))))
                .andExpect(model().attribute("mostrarAdministrador", false))
                .andExpect(content().string(containsString("No se ha podido completar el registro")))
                .andExpect(content().string(not(containsString(detalleInterno))))
                .andExpect(content().string(not(containsString("name=\"administrador\""))));

        verify(usuarioService).registrar(any(UsuarioData.class));
    }
}
