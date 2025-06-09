package org.com.pangolin;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

import org.com.pangolin.redistribuicao.ParametrosRedistribuicao;
import org.com.pangolin.redistribuicao.RedistribuicaoFinanciamento;
import org.com.pangolin.redistribuicao.RedistribuicaoSistemaAmortizacao;
import org.com.pangolin.redistribuicao.ResultadoRedistribuicao;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
        public static void main(String[] args) {

                // . Parametros do financiamento
                MathContext MC = new MathContext(2, RoundingMode.HALF_UP);
                ParametrosFinanciamento parametros = new ParametrosFinanciamento(
                        BigDecimal.valueOf(1500.0),
                        BigDecimal.valueOf(0.08), // 8% ao mês
                        12
                );
                BigDecimal valorPagoPrimeiraParcela = BigDecimal.valueOf(150.0);

                // 1. Calcular parcelas pelo sistema Price
                BigDecimal[] parcelasPrice = calcularParcelasPrice(parametros);
                BigDecimal valorTotalParcelas = Arrays.stream(parcelasPrice).reduce(BigDecimal.ZERO, BigDecimal::add);


                // Exibir informações do financiamento
                System.out.println("Financiamento de R$" + parametros.valorFinanciado() +
                                " em " + parametros.numParcelas() + " parcelas a " + parametros.taxaMensal().multiply(BigDecimal.valueOf(100)) + "% a.m.");
                System.out.printf("Valor total a pagar: R$%.2f%n", valorTotalParcelas);

                // 2.----------------- Calcular a decomposição original (Price) de principal e juros -----------
                BigDecimal[][] decomposicaoOriginal = decomporParcelasPrice(parametros);
                BigDecimal[] principalsOriginais = decomposicaoOriginal[0];
                BigDecimal[] jurosOriginais = decomposicaoOriginal[1];
                System.out.println("\nDecomposição original (Sistema Price):");
                imprimirTabelaAmortizacao(principalsOriginais, jurosOriginais, parcelasPrice, BigDecimal.ZERO);

                // 3.----------------- Pagamento Parcial Primeira Par ela-----------------------------
                System.out.println("Financiamento de R$" + parametros.valorFinanciado() );
                System.out.println("Pagamento parcial da primeira parcela: R$" + valorPagoPrimeiraParcela);
                BigDecimal saldoDevedorParcela = parcelasPrice[0].subtract(valorPagoPrimeiraParcela).setScale(2, RoundingMode.HALF_UP);
                System.out.println("Saldo Restante Parcela: R$" + saldoDevedorParcela);

                // 4. ----------------- Ajuste de Principal e Juros apos pagamento parcialprimeira parcela-----------------------------------

                BigDecimal principalPago = valorPagoPrimeiraParcela.subtract(jurosOriginais[0],MC);
                BigDecimal jurosPago= valorPagoPrimeiraParcela.subtract(principalPago,MC);
                System.out.println("Principal pago: R$" + principalPago);
                System.out.println("Juros pago: R$" + jurosPago);


                BigDecimal saldoPrincipal = parametros.valorFinanciado().subtract(principalPago);
                BigDecimal saldoJuros = (valorTotalParcelas.subtract(parametros.valorFinanciado())).subtract(jurosPago);

                System.out.println("Saldo Principal: R$" + saldoPrincipal);
                System.out.println("Saldo Juros: R$" + saldoJuros);

                // 5. ----------------- Decompor as parcelas restantes com saldo inicial (ajustado)-----------------------------
                BigDecimal[] parcelasRestantes = Arrays.copyOfRange(parcelasPrice, 1, parametros.numParcelas());
                BigDecimal[] parcelasAjustadas = new BigDecimal[parametros.numParcelas()];
                parcelasAjustadas[0] = saldoDevedorParcela;
                System.arraycopy(parcelasRestantes, 0, parcelasAjustadas, 0, parcelasRestantes.length);

                BigDecimal[][] decomposicaoRestante = decomporParcelasPriceComSaldoInicial(
                        saldoPrincipal, parametros.taxaMensal(),parcelasPrice[0], parametros.numParcelas() - 1);

                //imprimirTabelaAmortizacao(decomposicaoRestante[0], decomposicaoRestante[1], parcelasAjustadas);

                BigDecimal somaParcelasAjustadas = Arrays.stream(parcelasRestantes).reduce(BigDecimal.ZERO, BigDecimal::add);
                System.out.println("Saldo Parcelas: R$" + somaParcelasAjustadas);
                somaParcelasAjustadas = somaParcelasAjustadas.add(saldoDevedorParcela);

                RedistribuicaoFinanciamento redistribuicao = new RedistribuicaoFinanciamento( new ParametrosRedistribuicao(
                        somaParcelasAjustadas,
                        saldoPrincipal,
                        saldoJuros,
                        parcelasPrice[0],
                        parcelasRestantes.length,
                        parametros.taxaMensal(),
                        RedistribuicaoSistemaAmortizacao.PRICE,
                        true));

                ResultadoRedistribuicao resultado = redistribuicao.redistribuir();
               // resultado.validar(
               //         parcelasPrice,
                //        valorFinanciado,
               ///         valorTotalParcelas.subtract(valorFinanciado));

                System.out.println("\nRedistribuição com principal constante:");
                imprimirTabelaAmortizacao(
                        resultado.principal(),
                        resultado.juros(),
                        parcelasRestantes,
                        saldoDevedorParcela);


                RedistribuicaoFinanciamento redistribuicao_nao_constante = new RedistribuicaoFinanciamento( new ParametrosRedistribuicao(
                        somaParcelasAjustadas,
                        saldoPrincipal,
                        saldoJuros,
                        parcelasPrice[0],
                        parcelasRestantes.length,
                        parametros.taxaMensal(),
                        RedistribuicaoSistemaAmortizacao.SAC,
                        false));
                ResultadoRedistribuicao resultado_nao_constante = redistribuicao_nao_constante.redistribuir();
                System.out.println("\nRedistribuição com principal não-constante:");
                imprimirTabelaAmortizacao(
                        resultado_nao_constante.principal(),
                        resultado_nao_constante.juros(),
                        parcelasRestantes,
                        saldoDevedorParcela);

              /*  // 4. Ajustar as parcelas restantes
                BigDecimal[] parcelasRestantes = Arrays.copyOfRange(parcelasPrice, 1, numParcelas);
                BigDecimal[] parcelasAjustadas = new BigDecimal[numParcelas];
                parcelasAjustadas[0] = valorPagoPrimeiraParcela;
                System.arraycopy(parcelasRestantes, 0, parcelasAjustadas, 1, parcelasRestantes.length);

                BigDecimal[] principalsAjustados = new BigDecimal[numParcelas];
                BigDecimal[] jurosAjustados = new BigDecimal[numParcelas];
                principalsAjustados[0] = principalPago;
                jurosAjustados[0] = jurosPago;

                // Decompor as demais mantendo valor fixo
                BigDecimal[][] decomposicaoRestante = decomporParcelasPriceComSaldoInicial(
                        saldoPrincipal, taxaMensal, parcelasPrice[0], numParcelas - 1);

                System.arraycopy(decomposicaoRestante[0], 0, principalsAjustados, 1, numParcelas - 1);
                System.arraycopy(decomposicaoRestante[1], 0, jurosAjustados, 1, numParcelas - 1);
                
                BigDecimal somaParcelasAjustadas = Arrays.stream(parcelasAjustadas).reduce(BigDecimal.ZERO, BigDecimal::add);
               // BigDecimal saldoTotal = saldoPrincipal.add(saldoJuros);

                // Ajuste o saldoJuros para garantir igualdade
               // BigDecimal ajuste = somaParcelasAjustadas.subtract(saldoTotal);
              //  saldoJuros = saldoJuros.add(ajuste);

                BigDecimal saldoPrincipalAjustado = Arrays.stream(principalsAjustados).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal saldoJurosAjustado = Arrays.stream(jurosAjustados).reduce(BigDecimal.ZERO, BigDecimal::add);

                imprimirTabelaAmortizacao(principalsAjustados, jurosAjustados, parcelasAjustadas);


                BigDecimal saldoTotal = saldoPrincipalAjustado.add(saldoJurosAjustado);

// Ajuste final para garantir soma das parcelas == saldoPrincipal + saldoJuros
                BigDecimal ajuste = somaParcelasAjustadas.subtract(saldoTotal);
                if (ajuste.abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                        principalsAjustados[numParcelas - 1] = principalsAjustados[numParcelas - 1].subtract(ajuste);
                        principalsAjustados[numParcelas - 1] = principalsAjustados[numParcelas - 1].setScale(2, RoundingMode.HALF_UP);
                        jurosAjustados[numParcelas - 1] = parcelasAjustadas[numParcelas - 1].subtract(principalsAjustados[numParcelas - 1]).setScale(2, RoundingMode.HALF_UP);
                }

                // Agora imprima novamente para ver o ajuste
                imprimirTabelaAmortizacao(principalsAjustados, jurosAjustados, parcelasAjustadas);
                        // 3. Redistribuir os valores (como se fosse um refinanciamento)
                RedistribuicaoFinanciamento redistribuicao = new RedistribuicaoFinanciamento(
                                parcelasAjustadas,
                                saldoPrincipalAjustado,
                                saldoJurosAjustado,
                                false);

                RedistribuicaoFinanciamento.ResultadoRedistribuicao resultado = redistribuicao.redistribuir();
                resultado.validar(
                                parcelasPrice,
                                valorFinanciado,
                                valorTotalParcelas.subtract(valorFinanciado));

                System.out.println("\nRedistribuição com principal não-constante:");
                imprimirTabelaAmortizacao(
                                resultado.getPrincipals(),
                                resultado.getJuros(),
                                parcelasAjustadas);

                // 4. Redistribuir com principal constante
                RedistribuicaoFinanciamento redistribuicaoConstante = new RedistribuicaoFinanciamento(
                                parcelasAjustadas,
                                saldoPrincipal,
                                saldoJuros,
                                true);

                RedistribuicaoFinanciamento.ResultadoRedistribuicao resultadoConstante = redistribuicaoConstante.redistribuir();
                resultadoConstante.validar(
                                parcelasPrice,
                                valorFinanciado,
                                valorTotalParcelas.subtract(valorFinanciado));

                System.out.println("\nRedistribuição com principal constante:");
                imprimirTabelaAmortizacao(
                                resultadoConstante.getPrincipals(),
                                resultadoConstante.getJuros(),
                                parcelasAjustadas);
        */
        }



        record ParametrosFinanciamento(BigDecimal valorFinanciado, BigDecimal taxaMensal, int numParcelas) {}
        /**
         * Decompor parcelas do sistema Price com saldo inicial.
         *
         * @param saldoInicial  Saldo inicial do financiamento
         * @param taxa          Taxa de juros mensal
         * @param valorParcela  Valor da parcela fixa
         * @param numParcelas   Número total de parcelas
         * @return Array bidimensional com principais e juros de cada parcela
         */

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

        public static void imprimirTabelaAmortizacao(BigDecimal[] principals, BigDecimal[] juros, BigDecimal[] parcelas, BigDecimal residualParcela) {
                System.out.println("Parcela | Principal  | Juros      | Valor      | Saldo");
                System.out.println("-------|------------|------------|------------|------------");

                BigDecimal saldo = Arrays.stream(principals).reduce(BigDecimal.ZERO, BigDecimal::add);
                for (int i = 0; i < principals.length; i++) {
                        saldo = saldo.subtract(principals[i]);
                        System.out.printf("%6d | %10.2f | %10.2f | %10.2f | %10.2f%n",
                                        i + 1, principals[i], juros[i], parcelas[i], saldo.max(BigDecimal.ZERO));
                }

                System.out.printf("TOTAL  | %10.2f | %10.2f | %10.2f%n",
                                Arrays.stream(principals).reduce(BigDecimal.ZERO, BigDecimal::add),
                                Arrays.stream(juros).reduce(BigDecimal.ZERO, BigDecimal::add),
                                Arrays.stream(parcelas).reduce(BigDecimal.ZERO, BigDecimal::add).add(residualParcela));
        }
}
