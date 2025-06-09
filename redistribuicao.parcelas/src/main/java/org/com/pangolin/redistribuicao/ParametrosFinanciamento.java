package org.com.pangolin.redistribuicao;

import java.math.BigDecimal;

public record ParametrosFinanciamento(BigDecimal valorFinanciado, BigDecimal taxaMensal, int numParcelas) {
}
