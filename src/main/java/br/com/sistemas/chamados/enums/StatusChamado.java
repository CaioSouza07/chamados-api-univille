package br.com.sistemas.chamados.enums;

public enum StatusChamado {
    ABERTO("Aberto", "aberto"),
    EM_ATENDIMENTO("Em atendimento", "atendimento"),
    AGUARDANDO_CLIENTE("Aguardando cliente", "aguardando"),
    FECHADO("Fechado", "fechado");

    private final String label;
    private final String css;

    StatusChamado(String label, String css) {
        this.label = label;
        this.css = css;
    }

    public String getLabel() {
        return label;
    }

    public String getCss() {
        return css;
    }
}
