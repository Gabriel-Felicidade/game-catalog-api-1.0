package org.acme;

public enum ClassificacaoIndicativa {
    LIVRE("Livre para todos os públicos"),
    DEZ("Não recomendado para menores de 10 anos"),
    DOZE("Não recomendado para menores de 12 anos"),
    QUATORZE("Não recomendado para menores de 14 anos"),
    DEZESSEIS("Não recomendado para menores de 16 anos"),
    DEZOITO("Não recomendado para menores de 18 anos");

    private final String descricao;

    ClassificacaoIndicativa(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}