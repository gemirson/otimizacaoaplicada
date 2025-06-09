package org.com.pangolin.redistribuicao;

import java.math.BigDecimal;

public record ParametrosRedistribuicao(
        BigDecimal saldoDevedorParcelas,
        BigDecimal saldoPrincipal,
        BigDecimal saldoJuros,
        BigDecimal valorParcela,
        int quantidadeParcelas,
        BigDecimal taxaJuros,
        RedistribuicaoSistemaAmortizacao sistemaAmortizacao,
        boolean principalConstante) {

    public static  Builder builder() {
        return new Builder();
    }
    /**
     * Builder class for ParametrosRedistribuicao.
     * This class provides a fluent API to create instances of ParametrosRedistribuicao.
     */
    public static class Builder {
        private BigDecimal saldoDevedorParcelas;
        private BigDecimal saldoPrincipal;
        private BigDecimal saldoJuros;
        private BigDecimal valorParcela;
        private int quantidadeParcelas;
        private BigDecimal taxaJuros;
        private RedistribuicaoSistemaAmortizacao sistemaAmortizacao;
        private boolean principalConstante;

        public Builder saldoDevedorParcelas(BigDecimal saldoDevedorParcelas) {
            this.saldoDevedorParcelas = saldoDevedorParcelas;
            return this;
        }
        public Builder saldoPrincipal(BigDecimal saldoPrincipal) {
            this.saldoPrincipal = saldoPrincipal;
            return this;
        }
        public Builder saldoJuros(BigDecimal saldoJuros) {
            this.saldoJuros = saldoJuros;
            return this;
        }
        public Builder valorParcela(BigDecimal valorParcela) {
            this.valorParcela = valorParcela;
            return this;
        }
        public Builder quantidadeParcelas(int quantidadeParcelas) {
            this.quantidadeParcelas = quantidadeParcelas;
            return this;
        }
        public Builder taxaJuros(BigDecimal taxaJuros) {
            this.taxaJuros = taxaJuros;
            return this;
        }

        public Builder sistemaAmortizacao(RedistribuicaoSistemaAmortizacao sistemaAmortizacao) {
            this.sistemaAmortizacao = sistemaAmortizacao;
            return this;
        }

        public Builder principalConstante(boolean principalConstante) {
            this.principalConstante = principalConstante;
            return this;
        }

        public ParametrosRedistribuicao build() {
            return new ParametrosRedistribuicao(
                    saldoDevedorParcelas,
                    saldoPrincipal,
                    saldoJuros,
                    valorParcela,
                    quantidadeParcelas,
                    taxaJuros,
                    sistemaAmortizacao,
                    principalConstante
            );
        }
    }
}
