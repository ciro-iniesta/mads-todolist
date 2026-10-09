package madstodolist.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        value = HttpStatus.UNAUTHORIZED,
        reason = "No autorizado: no tienes suficientes permisos"
)
public class UsuarioNoAdministradorException extends RuntimeException {
}