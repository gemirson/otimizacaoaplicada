package org.com.pangolin.redistribuicao;

import java.math.BigDecimal;
import java.util.Arrays;

public record ResultadoRedistribuicao(
        BigDecimal[] principal,
        BigDecimal[] juros
        ) {

        public  static Builder builder() {
                return new Builder();
        }

        public static class Builder {
                private BigDecimal[] juros;
                private BigDecimal[] principal;
                private BigDecimal parcela;
                private BigDecimal saldoPrincipal;
                private BigDecimal saldoJuros;

                public  Builder principal(BigDecimal[] principal) {
                        this.principal = principal;
                        return this;
                }

                public Builder juros(BigDecimal[] juros) {
                        this.juros = juros;
                        return this;
                }
                public  Builder parcela(BigDecimal parcela) {
                        this.parcela = parcela;
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

                public ResultadoRedistribuicao build() {
                        validate();
                        return new ResultadoRedistribuicao(principal, juros);
                }
                private void validate() {
                        if (principal == null || juros == null) {
                                throw new IllegalArgumentException("Principal and juros must not be null");
                        }
                        if (principal.length != juros.length) {
                                throw new IllegalArgumentException("Principal and juros arrays must have the same length");
                        }
                        if (parcela == null) {
                                throw new IllegalArgumentException("Parcela must not be null");
                        }
                        if (saldoPrincipal == null || saldoJuros == null) {
                                throw new IllegalArgumentException("Saldo principal and saldo juros must not be null");
                        }
                        if (principal.length == 0) {
                                throw new IllegalArgumentException("Principal and juros arrays must not be empty");
                        }
                        if (juros.length == 0) {
                                throw new IllegalArgumentException("Principal and juros arrays must not be empty");
                        }

                        BigDecimal somaPrincipal = Arrays.stream(principal).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal somaJuros = Arrays.stream(juros).reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (somaPrincipal.subtract(saldoPrincipal).abs().compareTo(new BigDecimal("0.01")) > 0) {
                                throw new IllegalStateException("Soma dos principals não corresponde ao saldo");
                        }
                        if (somaJuros.subtract(saldoJuros).abs().compareTo(new BigDecimal("0.01")) > 0) {
                                throw new IllegalStateException("Soma dos juros não corresponde ao saldo");
                        }
                        for (int i = 0; i < principal.length - 1; i++) {
                                if (principal[i].add(juros[i]).subtract(parcela).abs().compareTo(new BigDecimal("0.01")) > 0) {
                                        throw new IllegalStateException("Parcela " + i + " não bate: principal + juros != parcela");
                                }
                        }
                }
        }
}
