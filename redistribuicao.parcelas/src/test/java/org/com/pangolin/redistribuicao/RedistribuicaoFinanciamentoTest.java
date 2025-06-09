package org.com.pangolin.redistribuicao;
import org.com.pangolin.Main;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

import static org.com.pangolin.Main.calcularParcelasPrice;
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
    public static BigDecimal[][] decomporParcelasPrice(ParametrosFinanciamento parametros) {
        BigDecimal[] principals = new BigDecimal[parametros.numParcelas()];
        BigDecimal[] juros = new BigDecimal[parametros.numParcelas()];
        BigDecimal saldoDevedor = parametros.valorFinanciado();

        BigDecimal valorParcela = calcularParcelasPrice(parametros)[0];

        for (int i = 0; i < parametros.numParcelas(); i++) {
            juros[i] = saldoDevedor.multiply(parametros.taxaMensal()).setScale(2, RoundingMode.HALF_UP);
            principals[i] = valorParcela.subtract(juros[i]).setScale(2, RoundingMode.HALF_UP);
            saldoDevedor = saldoDevedor.subtract(principals[i]);
        }

        // Ajuste de arredondamento para garantir que principal + juros == parcela
        for (int i = 0; i < parametros.numParcelas(); i++) {
            BigDecimal soma = principals[i].add(juros[i]);
            BigDecimal diff = valorParcela.subtract(soma).setScale(2, RoundingMode.HALF_UP);
            principals[i] = principals[i].add(diff);
            principals[i] = principals[i].setScale(2, RoundingMode.HALF_UP);
            juros[i] = valorParcela.subtract(principals[i]).setScale(2, RoundingMode.HALF_UP);
        }

        // Ajuste final para última parcela garantir saldoDevedor zero
        BigDecimal somaPrincipals = Arrays.stream(principals).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ajusteFinal = parametros.valorFinanciado().subtract(somaPrincipals).setScale(2, RoundingMode.HALF_UP);
        if (ajusteFinal.abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            principals[parametros.numParcelas()- 1] = principals[parametros.numParcelas() - 1].add(ajusteFinal);
            principals[parametros.numParcelas() - 1] = principals[parametros.numParcelas()- 1].setScale(2, RoundingMode.HALF_UP);
            juros[parametros.numParcelas() - 1] = valorParcela.subtract(principals[parametros.numParcelas()- 1]).setScale(2, RoundingMode.HALF_UP);
        }

        return new BigDecimal[][]{principals, juros};
    }
    public static BigDecimal[] calcularParcelasPrice(ParametrosFinanciamento parametros) {
        BigDecimal[] parcelas = new BigDecimal[parametros.numParcelas()];
        BigDecimal um = BigDecimal.ONE;
        BigDecimal taxaMaisUm = um.add(parametros.taxaMensal());
        BigDecimal potencia = taxaMaisUm.pow(parametros.numParcelas());
        BigDecimal numerador = parametros.taxaMensal().multiply(potencia);
        BigDecimal denominador = potencia.subtract(um);
        BigDecimal valorParcela = parametros.valorFinanciado().multiply(numerador).divide(denominador, 2, RoundingMode.HALF_UP);

        valorParcela = valorParcela.setScale(2, RoundingMode.HALF_UP);
        Arrays.fill(parcelas, valorParcela);
        return parcelas;
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
    MathContext MC = new MathContext(2, RoundingMode.HALF_UP);
    BigDecimal saldoPrincipal = new BigDecimal("1500");
    BigDecimal taxaMensal = new BigDecimal("0.02");
    BigDecimal saldoJuros = BigDecimal.valueOf(50,40); // 50.40
    BigDecimal valorParcela = BigDecimal.valueOf(520,13); // 52.00
    BigDecimal saldoDevedorParcelas = BigDecimal.valueOf(1040,26); // 10   4.00
    int numParcelas = 3;

    ParametrosFinanciamento parametros = new ParametrosFinanciamento(
            saldoPrincipal,
            taxaMensal,
            numParcelas
    );

    BigDecimal[][] decomposicaoOriginal = decomporParcelasPrice(parametros);

    BigDecimal[] principalsOriginais = decomposicaoOriginal[0];
    BigDecimal[] jurosOriginais = decomposicaoOriginal[1];

    for (int i = 0; i < principalsOriginais.length; i++) {
        System.out.printf("Parcela %2d | Principal: %8.2f | Juros: %8.2f | Valor: %8.2f\n",
                i + 1,
                principalsOriginais [i],
                jurosOriginais[i],
                principalsOriginais [i].add(jurosOriginais[i])
        );
    }

    // Ajusta os juros para o saldo devedor

    // Amortiza parcialmente a primeira parcela, priorizando os juros
    BigDecimal valorPagoPrimeiraParcela = new BigDecimal("500");
    BigDecimal principalAmortizado  = valorPagoPrimeiraParcela.subtract(jurosOriginais[0],MC);
    BigDecimal jurosAmortizado  = valorPagoPrimeiraParcela.subtract(principalAmortizado,MC);



    // Calcula saldos após amortização parcial
    saldoPrincipal = saldoPrincipal.subtract(principalAmortizado, MC);
    saldoJuros = saldoJuros.subtract(jurosAmortizado,MC);
    BigDecimal saldoParcela = valorParcela.subtract(valorPagoPrimeiraParcela,MC);
    saldoDevedorParcelas = saldoDevedorParcelas.add(saldoParcela,MC);
    System.out.printf("Principal: %8.2f | Juros: %8.2f ",
            saldoPrincipal,
            saldoJuros.abs()
    );

    //assertEquals(new BigDecimal("300.00"),  jurosAmortizado.setScale(2), "Saldo de juros após amortização parcial");
    //assertEquals(new BigDecimal("1350.00"), principalsOriginais[1].setScale(2), "Saldo de principal após amortização parcial");
    //assertEquals(new BigDecimal("150.00"), saldoParcela.setScale(2), "Saldo da parcela após amortização parcial");

    ParametrosRedistribuicao parametros_r = ParametrosRedistribuicao.builder()
            .saldoPrincipal(saldoPrincipal)
            .saldoJuros(saldoJuros)
            .quantidadeParcelas(numParcelas - 1) // Uma parcela foi amortizada
            .valorParcela(valorParcela)
            .sistemaAmortizacao(RedistribuicaoSistemaAmortizacao.PRICE)
            .taxaJuros(taxaMensal)
            .saldoDevedorParcelas(saldoDevedorParcelas) // opcional, se necessário
            .build();


    // Act
    RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros_r);
    var resultado = r.redistribuir();
    // Assert
    assertResultado(resultado, parametros_r);
    // Assert


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
