package br.com.sistemas.chamados.exception;

/**
 * Parâmetro de requisição com valor não aceito (ex.: campo de ordenação inexistente). Vira HTTP 400.
 */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String message) {
        super(message);
    }
}
