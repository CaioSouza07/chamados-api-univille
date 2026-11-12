package br.com.sistemas.chamados.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(RecursoNaoEncontradoException e) {
        return montar(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResposta> regraNegocio(RegraNegocioException e) {
        return montar(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(error-> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return montar(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhes);
    }

    /** Corpo da requisição ilegível: JSON malformado ou valor que não existe no enum (ex.: "status": "XYZ"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException e) {
        if (e.getMostSpecificCause() instanceof InvalidFormatException formato && formato.getTargetType().isEnum()) {
            String campo = formato.getPath().isEmpty() ? "valor" : formato.getPath().getLast().getPropertyName();
            return montar(HttpStatus.BAD_REQUEST, "Dados inválidos",
                    List.of(campo + ": " + valorNaoAceito(formato.getValue(), formato.getTargetType())));
        }
        return montar(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado", List.of());
    }

    /** Parâmetro de URL com tipo errado (ex.: ?status=XYZ ou ?clienteId=abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> parametroComTipoErrado(MethodArgumentTypeMismatchException e) {
        Class<?> tipo = e.getRequiredType();
        String detalhe = tipo != null && tipo.isEnum()
                ? valorNaoAceito(e.getValue(), tipo)
                : "valor '" + e.getValue() + "' inválido";
        return montar(HttpStatus.BAD_REQUEST, "Parâmetro inválido", List.of(e.getName() + ": " + detalhe));
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(ParametroInvalidoException e) {
        return montar(HttpStatus.BAD_REQUEST, e.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> violacaoIntegridade(DataIntegrityViolationException e) {
        return montar(HttpStatus.CONFLICT, "Operação não permitida: o registro está vinculado a outros dados do sistema", List.of());
    }

    // 409: a requisição é válida, mas conflita com o estado atual do chamado (fechado).
    @ExceptionHandler(ChamadoFechadoException.class)
    public ResponseEntity<ErroResposta> chamadoFechado(ChamadoFechadoException e) {
        return montar(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }

    private static String valorNaoAceito(Object valor, Class<?> tipoEnum) {
        String aceitos = Arrays.stream(tipoEnum.getEnumConstants())
                .map(constante -> ((Enum<?>) constante).name())
                .collect(Collectors.joining(", "));
        return "valor '" + valor + "' não aceito. Valores aceitos: " + aceitos;
    }

    private ResponseEntity<ErroResposta> montar(HttpStatus status, String erro, List<String> detalhes) {
        return ResponseEntity.status(status).body(new ErroResposta(LocalDateTime.now(), status.value(), erro, detalhes));
    }
}
