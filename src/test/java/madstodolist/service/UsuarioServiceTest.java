package madstodolist.service;

import madstodolist.dto.UsuarioData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    // Método para inicializar los datos de prueba en la BD
    // Devuelve el identificador del usuario de la BD
    Long addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setNombre("Usuario Ejemplo");
        usuario.setPassword("123");
        UsuarioData nuevoUsuario = usuarioService.registrar(usuario);
        return nuevoUsuario.getId();
    }

    @Test
    public void servicioLoginUsuario() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // WHEN
        // intentamos logear un usuario y contraseña correctos
        UsuarioService.LoginStatus loginStatus1 = usuarioService.login("user@ua", "123");

        // intentamos logear un usuario correcto, con una contraseña incorrecta
        UsuarioService.LoginStatus loginStatus2 = usuarioService.login("user@ua", "000");

        // intentamos logear un usuario que no existe,
        UsuarioService.LoginStatus loginStatus3 = usuarioService.login("pepito.perez@gmail.com", "12345678");

        // THEN

        // el valor devuelto por el primer login es LOGIN_OK,
        assertThat(loginStatus1).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);

        // el valor devuelto por el segundo login es ERROR_PASSWORD,
        assertThat(loginStatus2).isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);

        // y el valor devuelto por el tercer login es USER_NOT_FOUND.
        assertThat(loginStatus3).isEqualTo(UsuarioService.LoginStatus.USER_NOT_FOUND);
    }

    @Test
    public void servicioRegistroUsuario() {
        // WHEN
        // Registramos un usuario con un e-mail no existente en la base de datos,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba2@gmail.com");
        usuario.setPassword("12345678");

        usuarioService.registrar(usuario);

        // THEN
        // el usuario se añade correctamente al sistema.

        UsuarioData usuarioBaseDatos = usuarioService.findByEmail("usuario.prueba2@gmail.com");
        assertThat(usuarioBaseDatos).isNotNull();
        assertThat(usuarioBaseDatos.getEmail()).isEqualTo("usuario.prueba2@gmail.com");
    }

    @Test
    public void servicioRegistroUsuarioExcepcionConNullPassword() {
        // WHEN, THEN
        // Si intentamos registrar un usuario con un password null,
        // se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }


    @Test
    public void servicioRegistroUsuarioExcepcionConEmailRepetido() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // THEN
        // Si registramos un usuario con un e-mail ya existente en la base de datos,
        // , se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("12345678");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }

    @Test
    public void servicioRegistroUsuarioDevuelveUsuarioConId() {

        // WHEN
        // Si registramos en el sistema un usuario con un e-mail no existente en la base de datos,
        // y un password no nulo,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");
        usuario.setPassword("12345678");

        UsuarioData usuarioNuevo = usuarioService.registrar(usuario);

        // THEN
        // se actualiza el identificador del usuario

        assertThat(usuarioNuevo.getId()).isNotNull();

        // con el identificador que se ha guardado en la BD.

        UsuarioData usuarioBD = usuarioService.findById(usuarioNuevo.getId());
        assertThat(usuarioBD).isEqualTo(usuarioNuevo);
    }

    @Test
    public void servicioConsultaUsuarioDevuelveUsuario() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // recuperamos un usuario usando su e-mail,

        UsuarioData usuario = usuarioService.findByEmail("user@ua");

        // THEN
        // el usuario obtenido es el correcto.

        assertThat(usuario.getId()).isEqualTo(usuarioId);
        assertThat(usuario.getEmail()).isEqualTo("user@ua");
        assertThat(usuario.getNombre()).isEqualTo("Usuario Ejemplo");
    }

    @Test
    public void servicioListarUsuarios() {
        // GIVEN
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.listado@gmail.com");
        usuario.setPassword("12345");
        usuarioService.registrar(usuario);

        // WHEN
        List<UsuarioData> usuarios = usuarioService.allUsuarios();

        // THEN
        // Comprobamos que al menos recuperamos 1 elemento tras el registro
        assertThat(usuarios).hasSizeGreaterThan(0);
    }

    // Prepara los datos; cada test decide cuándo registrarlos y si serán de administrador.
    private UsuarioData datosRegistro(String email) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setPassword("12345678");
        return usuario;
    }

    @Test
    public void noExisteAdministradorConBaseDeDatosVacia() {
        // @Sql limpia la base de datos antes de cada test.
        assertThat(usuarioService.existeAdministrador()).isFalse();
    }

    @Test
    public void registroSinSolicitarAdministradorCreaUsuarioNormal() {
        // No asignamos administrador: comprobamos el valor por defecto al guardar.
        UsuarioData registrado = usuarioService.registrar(datosRegistro("normal@ejemplo.com"));

        UsuarioData recuperado = usuarioService.findById(registrado.getId());
        assertThat(recuperado).isNotNull();
        assertThat(recuperado.isAdministrador()).isFalse();
        assertThat(usuarioService.existeAdministrador()).isFalse();

        // Un usuario normal no impide registrar después al primer administrador.
        UsuarioData administrador = datosRegistro("admin@ejemplo.com");
        administrador.setAdministrador(true);
        usuarioService.registrar(administrador);
        assertThat(usuarioService.existeAdministrador()).isTrue();
    }

    @Test
    public void registroAdministradorConservaElRolAlRecuperarlo() {
        UsuarioData administrador = datosRegistro("admin@ejemplo.com");
        administrador.setAdministrador(true);

        UsuarioData registrado = usuarioService.registrar(administrador);

        assertThat(registrado.getId()).isNotNull();
        assertThat(registrado.isAdministrador()).isTrue();
        assertThat(usuarioService.existeAdministrador()).isTrue();

        // Estas consultas comprueban también la conversión entre entidad y DTO.
        UsuarioData porId = usuarioService.findById(registrado.getId());
        UsuarioData porEmail = usuarioService.findByEmail("admin@ejemplo.com");
        assertThat(porId).isNotNull();
        assertThat(porEmail).isNotNull();
        assertThat(porId.isAdministrador()).isTrue();
        assertThat(porEmail.isAdministrador()).isTrue();
        assertThat(porEmail.getId()).isEqualTo(registrado.getId());
    }

    @Test
    public void rechazarSegundoAdministradorNoLoGuarda() {
        UsuarioData primero = datosRegistro("admin@ejemplo.com");
        primero.setAdministrador(true);
        usuarioService.registrar(primero);

        UsuarioData segundo = datosRegistro("otro-admin@ejemplo.com");
        segundo.setAdministrador(true);

        UsuarioServiceException excepcion = Assertions.assertThrows(
                UsuarioServiceException.class,
                () -> usuarioService.registrar(segundo));

        assertThat(excepcion.getMessage()).isEqualTo("Ya existe un usuario administrador");
        assertThat(usuarioService.findByEmail("otro-admin@ejemplo.com")).isNull();
        assertThat(usuarioService.allUsuarios()).hasSize(1);
        assertThat(usuarioService.findByEmail("admin@ejemplo.com").isAdministrador()).isTrue();
    }

    @Test
    public void administradorExistentePermiteRegistrarVariosUsuariosNormales() {
        UsuarioData administrador = datosRegistro("admin@ejemplo.com");
        administrador.setAdministrador(true);
        usuarioService.registrar(administrador);

        usuarioService.registrar(datosRegistro("normal1@ejemplo.com"));
        usuarioService.registrar(datosRegistro("normal2@ejemplo.com"));

        assertThat(usuarioService.allUsuarios()).hasSize(3);
        assertThat(usuarioService.findByEmail("normal1@ejemplo.com").isAdministrador()).isFalse();
        assertThat(usuarioService.findByEmail("normal2@ejemplo.com").isAdministrador()).isFalse();
        assertThat(usuarioService.existeAdministrador()).isTrue();
    }

    @Test
    public void administradorDebeValidarSuPasswordParaIniciarSesion() {
        UsuarioData administrador = datosRegistro("admin@ejemplo.com");
        administrador.setAdministrador(true);
        usuarioService.registrar(administrador);

        assertThat(usuarioService.login("admin@ejemplo.com", "incorrecta"))
                .isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);
        assertThat(usuarioService.login("admin@ejemplo.com", "12345678"))
                .isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
    }
}
