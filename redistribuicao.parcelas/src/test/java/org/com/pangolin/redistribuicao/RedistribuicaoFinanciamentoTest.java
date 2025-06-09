package org.com.pangolin.redistribuicao;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RedistribuicaoFinanciamentoTest {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");

    private void assertResultado(ResultadoRedistribuicao resultado,
                                 ParametrosRedistribuicao parametros) {
        BigDecimal[] principals = resultado.principal();
        BigDecimal[] juros = resultado.juros();
        BigDecimal[] parcelas = new BigDecimal[parametros.quantidadeParcelas()];
        Arrays.fill(parcelas, parametros.valorParcela());

        BigDecimal somaPrincipals = Arrays.stream(principals).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaJuros = Arrays.stream(juros).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertTrue(somaPrincipals.subtract(parametros.saldoPrincipal()).abs().compareTo(TOLERANCIA) <= 0,
                "Soma dos principals não corresponde ao saldo principal");

        assertTrue(somaJuros.subtract(parametros.saldoJuros()).abs().compareTo(TOLERANCIA) <= 0,
                "Soma dos juros não corresponde ao saldo de juros");

        for (int i = 0; i < principals.length; i++) {
            BigDecimal soma = principals[i].add(juros[i]);
            assertTrue(soma.subtract(parcelas[i]).abs().compareTo(TOLERANCIA) <= 0,
                    "Parcela " + (i+1) + " não bate: principal + juros != parcela");
        }
    }
    public static BigDecimal[][] decomporParcelasPriceComSaldoInicial(
            BigDecimal saldoInicial, BigDecimal taxa, BigDecimal valorParcela, int numParcelas) {
        BigDecimal[] principals = new BigDecimal[numParcelas];
        BigDecimal[] juros = new BigDecimal[numParcelas];
        BigDecimal saldoDevedor = saldoInicial;

        for (int i = 0; i < numParcelas; i++) {
            juros[i] = saldoDevedor.multiply(taxa).setScale(2, RoundingMode.HALF_UP);
            principals[i] = valorParcela.subtract(juros[i]).setScale(2, RoundingMode.HALF_UP);
            saldoDevedor = saldoDevedor.subtract(principals[i]);
        }



        // Ajuste final para garantir saldoDevedor zero
        BigDecimal somaPrincipals = Arrays.stream(principals).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ajusteFinal = saldoInicial.subtract(somaPrincipals).setScale(2, RoundingMode.HALF_UP);
        if (ajusteFinal.abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            principals[numParcelas - 1] = principals[numParcelas - 1].add(ajusteFinal);
            principals[numParcelas - 1] = principals[numParcelas - 1].setScale(2, RoundingMode.HALF_UP);
            juros[numParcelas - 1] = valorParcela.subtract(principals[numParcelas - 1]).setScale(2, RoundingMode.HALF_UP);
        }

        return new BigDecimal[][]{principals, juros};
    }

    @Test
    void testCenario1_Price_ParcelasIguais() {
        // Arrange
        ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
                .saldoPrincipal(new BigDecimal("3000"))
                .saldoJuros(new BigDecimal("600"))
                .quantidadeParcelas(3)
                .valorParcela(new BigDecimal("1200"))
                .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.PRICE)
                .taxaJuros(new BigDecimal("0.02"))
                .saldoDevedorParcelas( new BigDecimal("3600")) // opcional, se necessário
                .build();
        // Act
        RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
        var resultado = r.redistribuir();
        // Assert
        assertResultado(resultado, parametros);
    }
@Test
void testCenario2_SAC_ParcelasIguais() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("5000"))
            .saldoJuros(new BigDecimal("1000"))
            .quantidadeParcelas(5)
            .valorParcela(new BigDecimal("1200"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.SAC)
            .taxaJuros(new BigDecimal("0.015"))
            .saldoDevedorParcelas(new BigDecimal("6000")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}

@Test
void testCenario3_SFF_ParcelasNaoRedondas() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("4500"))
            .saldoJuros(new BigDecimal("789.23"))
            .quantidadeParcelas(6)
            .valorParcela(new BigDecimal("881.54"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.SFF)
            .taxaJuros(new BigDecimal("0.018"))
            .saldoDevedorParcelas(new BigDecimal("5289.23")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}

@Test
void testCenario4_Price_UmaParcela() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("1000"))
            .saldoJuros(new BigDecimal("80"))
            .quantidadeParcelas(1)
            .valorParcela(new BigDecimal("1080"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.PRICE)
            .taxaJuros(new BigDecimal("0.08"))
            .saldoDevedorParcelas( new BigDecimal("1080")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}

@Test
void testCenario5_SAC_TaxaZero() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("12000"))
            .saldoJuros(new BigDecimal("0"))
            .quantidadeParcelas(12)
            .valorParcela(new BigDecimal("1000"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.SAC)
            .taxaJuros(new BigDecimal("0.0"))
            .saldoDevedorParcelas( new BigDecimal("12000")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}

@Test
void testCenario6_Price_TaxaAlta() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("10000"))
            .saldoJuros(new BigDecimal("8000"))
            .quantidadeParcelas(24)
            .valorParcela(new BigDecimal("750"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.PRICE)
            .taxaJuros(new BigDecimal("0.03"))
            .saldoDevedorParcelas(new BigDecimal("18000")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}

@Test
void testCenario7_SAC_ParcelasDecrescentes() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("6000"))
            .saldoJuros(new BigDecimal("900"))
            .quantidadeParcelas(6)
            .valorParcela(new BigDecimal("1150"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.SAC)
            .taxaJuros(new BigDecimal("0.025"))
            .saldoDevedorParcelas( new BigDecimal("'6900")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}

@Test
void testCenario8_SFF_2Parcelas_TaxaPequena() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("2000"))
            .saldoJuros(new BigDecimal("20"))
            .quantidadeParcelas(2)
            .valorParcela(new BigDecimal("1010"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.SFF)
            .taxaJuros(new BigDecimal("0.01"))
            .saldoDevedorParcelas( new BigDecimal("2020")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}

@Test
void testCenario9_Price_PrimeiraParcelaDiferente() {
    // Arrange

    BigDecimal saldoPrincipal = new BigDecimal("1500");
    BigDecimal taxaMensal = new BigDecimal("0.02");
    BigDecimal saldoJuros = new BigDecimal("300");
    BigDecimal valorParcela = new BigDecimal("650");
    int numParcelas = 2;

    BigDecimal[] parcelasPrice = new BigDecimal[]{new BigDecimal("650"), new BigDecimal("650")};
    BigDecimal[][] decomposicaoRestante = decomporParcelasPriceComSaldoInicial(
            saldoPrincipal, taxaMensal, parcelasPrice[0], numParcelas - 1);

    BigDecimal[] principalsAjustados = new BigDecimal[numParcelas];
    BigDecimal[] jurosAjustados = new BigDecimal[numParcelas];


    System.arraycopy(decomposicaoRestante[0], 0, principalsAjustados, 1, numParcelas - 1);
    System.arraycopy(decomposicaoRestante[1], 0, jurosAjustados, 1, numParcelas - 1);

    // Amortiza parcialmente a primeira parcela, priorizando os juros
    BigDecimal valorPagoPrimeiraParcela = new BigDecimal("500");
    BigDecimal jurosAmortizado = jurosAjustados[0].subtract(valorPagoPrimeiraParcela);
    BigDecimal principalAmortizado = valorPagoPrimeiraParcela.subtract(jurosAmortizado).max(BigDecimal.ZERO);


    // Calcula saldos após amortização parcial
    saldoPrincipal = saldoPrincipal.subtract(principalAmortizado);
    saldoJuros = saldoJuros.subtract(jurosAmortizado);
    BigDecimal saldoParcela = valorParcela.subtract(valorPagoPrimeiraParcela);

    assertEquals(new BigDecimal("300.00"), jurosAjustados[0].setScale(2), "Saldo de juros após amortização parcial");
    assertEquals(new BigDecimal("1350.00"), saldoPrincipal.setScale(2), "Saldo de principal após amortização parcial");
    assertEquals(new BigDecimal("150.00"), saldoParcela.setScale(2), "Saldo da parcela após amortização parcial");


    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(saldoPrincipal)
            .saldoJuros(saldoJuros)
            .quantidadeParcelas(2)
            .valorParcela(valorParcela)
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.PRICE)
            .taxaJuros(taxaMensal)
            .saldoDevedorParcelas( new BigDecimal("1450")) // opcional, se necessário
            // .valorPagoPrimeiraParcela(new BigDecimal("500")) // se suportado
            .build();

    // Act
   // RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    //var resultado = r.redistribuir();
    // Assert
   // assertResultado(resultado, parametros);
    // Assert
    assertEquals(new BigDecimal("300.00"), saldoJuros.setScale(2), "Saldo de juros após amortização parcial");
    assertEquals(new BigDecimal("1350.00"), saldoPrincipal.setScale(2), "Saldo de principal após amortização parcial");
    assertEquals(new BigDecimal("150.00"), saldoParcela.setScale(2), "Saldo da parcela após amortização parcial");

}

@Test
void testCenario10_SAC_JurosZero() {
    // Arrange
    ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
            .saldoPrincipal(new BigDecimal("9000"))
            .saldoJuros(new BigDecimal("0"))
            .quantidadeParcelas(9)
            .valorParcela(new BigDecimal("1000"))
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.SAC)
            .taxaJuros(new BigDecimal("0.0"))
            .saldoDevedorParcelas( new BigDecimal("9000")) // opcional, se necessário
            .build();
    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros);
}
}
