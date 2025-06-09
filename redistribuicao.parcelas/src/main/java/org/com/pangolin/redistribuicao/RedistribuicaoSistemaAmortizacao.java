package org.com.pangolin.redistribuicao;

public enum RedistribuicaoSistemaAmortizacao {
    SAC("SAC"),
    PRICE("PRICE"),
    SFF("SFF");

    private final String descricao;

    RedistribuicaoSistemaAmortizacao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
