package br.com.sistemas.chamados.enums;

public enum Prioridade {
    BAIXA("Baixa", "baixa"),
    MEDIA("Média", "media"),
    ALTA("Alta", "alta"),
    CRITICA("Crítica", "critica");

    private final String label;
    private final String css;

    Prioridade(String label, String css) {
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
