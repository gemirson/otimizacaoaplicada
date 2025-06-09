package org.com.pangolin.redistribuicao;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;


/**
 * Classe responsável por redistribuir os valores de principal e juros das parcelas de um financiamento,
 * conforme o sistema de amortização definido nos parâmetros.
 *
 * <p>
 * Suporta diferentes sistemas de amortização, como PRICE, SAC e SFF, utilizando otimização numérica
 * para garantir que as restrições financeiras sejam respeitadas.
 * </p>
 *
 * <h2>Exemplo de uso geral</h2>
 * <pre>
 *     ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
 *         .saldoPrincipal(new BigDecimal("3000"))
 *         .saldoJuros(new BigDecimal("600"))
 *         .quantidadeParcelas(3)
 *         .valorParcela(new BigDecimal("1200"))
 *         .sistemaAmortizacao(SistemaAmortizacao.PRICE)
 *         .taxaJuros(new BigDecimal("0.02"))
 *         .build();
 *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
 *     RedistribuicaoFinanciamento.ResultadoRedistribuicao resultado = r.redistribuir();
 *     System.out.println(Arrays.toString(resultado.getPrincipals())); // Principais redistribuídos
 *     System.out.println(Arrays.toString(resultado.getJuros()));      // Juros redistribuídos
 * </pre>
 *
 * <h2>Exemplo para SAC</h2>
 * <pre>
 *     parametros = parametros.toBuilder().sistemaAmortizacao(SistemaAmortizacao.SAC).build();
 *     r = new RedistribuicaoFinanciamento(parametros);
 *     resultado = r.redistribuir();
 *     // Principais constantes, juros decrescentes
 * </pre>
 *
 * <h2>Validação dos resultados</h2>
 * <pre>
 *     resultado.validar(
 *         new BigDecimal[]{new BigDecimal("1200"), new BigDecimal("1200"), new BigDecimal("1200")},
 *         new BigDecimal("3000"),
 *         new BigDecimal("600")
 *     );
 * </pre>
 */
public class RedistribuicaoFinanciamento {

    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_EVEN);

    private final ParametrosRedistribuicao parametros;

    public RedistribuicaoFinanciamento(ParametrosRedistribuicao parametros) {
        this.parametros = parametros;
        validarDados();
    }

    private void validarDados() {
        BigDecimal totalEsperado = parametros.saldoPrincipal().add(parametros.saldoJuros());
        if (parametros.saldoDevedorParcelas().compareTo(totalEsperado) != 0) {
            throw new IllegalArgumentException(
                    String.format("Soma das parcelas (%s) deve igualar saldoPrincipal + saldoJuros (%s)",
                            parametros.saldoDevedorParcelas(), totalEsperado));
        }
    }

    /**
     * Redistribui os valores de principal e juros das parcelas conforme o sistema de amortização definido.
     * <p>
     * O método seleciona a estratégia de redistribuição de acordo com o sistema de amortização informado nos parâmetros.
     * </p>
     *
     * <b>Exemplo de uso:</b>
     * <pre>
     *     ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
     *         .saldoPrincipal(new BigDecimal("3000"))
     *         .saldoJuros(new BigDecimal("600"))
     *         .quantidadeParcelas(3)
     *         .valorParcela(new BigDecimal("1200"))
     *         .sistemaAmortizacao(SistemaAmortizacao.PRICE)
     *         .build();
     *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
     *     ResultadoRedistribuicao resultado = r.redistribuir();
     *     // resultado.getPrincipals() e resultado.getJuros() terão os valores redistribuídos conforme o sistema
     * </pre>
     *
     * <b>Exemplo prático para SAC:</b>
     * <pre>
     *     parametros = parametros.toBuilder().sistemaAmortizacao(SistemaAmortizacao.SAC).build();
     *     r = new RedistribuicaoFinanciamento(parametros);
     *     resultado = r.redistribuir();
     *     // Principais constantes, juros decrescentes
     * </pre>
     *
     * @return ResultadoRedistribuicao contendo os arrays de principais e juros redistribuídos.
     */
    public ResultadoRedistribuicao redistribuir() {
        return switch (parametros.sistemaAmortizacao()) {
            case SAC -> redistribuirComSAC();
            case PRICE, SFF -> redistribuirComPrincipalConstante();
            // Adicione outros casos se necessário
        };

    }

    private ResultadoRedistribuicao redistribuirComPrincipalConstante() {
        return rodarOtimizacao(this::funcaoObjetivo);
    }

    private ResultadoRedistribuicao redistribuirComPrincipalVariavel() {
        return rodarOtimizacao(this::funcaoObjetivoPrincipalVariavel);
    }

    // Para SAC, use:
    private ResultadoRedistribuicao redistribuirComSAC() {
        return rodarOtimizacao(this::funcaoObjetivoSAC);
    }

    /**
     * Executa a otimização para redistribuição dos principais e juros das parcelas.
     * <p>
     * Utiliza o algoritmo Nelder-Mead para encontrar a melhor distribuição dos principais,
     * de acordo com a função objetivo fornecida, respeitando as restrições do problema.
     * </p>
     *
     * <b>Exemplo de uso:</b>
     * <pre>
     *     ParametrosRedistribuicao parametros = ...;
     *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
     *     ResultadoRedistribuicao resultado = r.rodarOtimizacao(r::funcaoObjetivo);
     *     BigDecimal[] principals = resultado.getPrincipals();
     *     BigDecimal[] juros = resultado.getJuros();
     * </pre>
     *
     * <b>Exemplo prático:</b>
     * <pre>
     *     // Suponha saldoPrincipal = 3000, saldoJuros = 600, quantidadeParcelas = 3, valorParcela = 1200
     *     // A função objetivo pode ser r::funcaoObjetivo, r::funcaoObjetivoSAC, etc.
     *     ParametrosRedistribuicao parametros = ParametrosRedistribuicao.builder()
     *         .saldoPrincipal(new BigDecimal("3000"))
     *         .saldoJuros(new BigDecimal("600"))
     *         .quantidadeParcelas(3)
     *         .valorParcela(new BigDecimal("1200"))
     *         .build();
     *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
     *     ResultadoRedistribuicao resultado = r.rodarOtimizacao(r::funcaoObjetivo);
     *     // resultado.getPrincipals() e resultado.getJuros() terão os valores otimizados
     * </pre>
     *
     * @param funcaoObjetivo Função objetivo a ser minimizada durante a otimização.
     * @return ResultadoRedistribuicao contendo os arrays de principais e juros otimizados.
     */

    private ResultadoRedistribuicao rodarOtimizacao(MultivariateFunction funcaoObjetivo) {
        int n = parametros.quantidadeParcelas();
        BigDecimal valorParcela = parametros.valorParcela();

        ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(1e-12, 1e-12);
        SimplexOptimizer optimizer = new SimplexOptimizer(checker);
        NelderMeadSimplex simplex = new NelderMeadSimplex(n);

        ObjectiveFunction objective = new ObjectiveFunction(funcaoObjetivo);

        PointValuePair solution = optimizer.optimize(
                new MaxEval(100000),
                objective,
                GoalType.MINIMIZE,
                simplex,
                new InitialGuess(gerarChuteInicial()),
                new NonNegativeConstraint(true)
        );

        double[] principalsDouble = solution.getPoint();
        BigDecimal[] principals = new BigDecimal[n];
        BigDecimal[] juros = new BigDecimal[n];

        for (int i = 0; i < n; i++) {
            principals[i] = new BigDecimal(principalsDouble[i], MC).setScale(2, RoundingMode.HALF_EVEN);
            juros[i] = valorParcela.subtract(principals[i]).setScale(2, RoundingMode.HALF_EVEN);
        }

        // Ajuste final para garantir que as somas batam exatamente
        ajustarUltimaParcela(principals, juros, parametros.saldoPrincipal(), parametros.saldoJuros(), valorParcela);

        return  ResultadoRedistribuicao.builder()
                .principal(principals)
                .juros(juros)
                .parcela(valorParcela)
                .saldoPrincipal(parametros.saldoPrincipal())
                .saldoJuros(parametros.saldoJuros())
                .build();

    }


    /**
     * Gera um chute inicial para o vetor de principais a ser usado na otimização.
     * <p>
     * O chute inicial consiste em dividir o saldo principal igualmente entre todas as parcelas,
     * servindo como ponto de partida para o algoritmo de otimização.
     * </p>
     *
     * <b>Exemplo de uso:</b>
     * <pre>
     *     ParametrosRedistribuicao parametros = ...;
     *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
     *     double[] chute = r.gerarChuteInicial();
     *     // chute terá tamanho igual à quantidade de parcelas, cada valor igual a saldoPrincipal/n
     * </pre>
     *
     * <b>Exemplo prático:</b>
     * <pre>
     *     // Suponha saldoPrincipal = 3000, quantidadeParcelas = 3
     *     // O chute inicial será [1000.0, 1000.0, 1000.0]
     * </pre>
     *
     * @return Vetor double[] com valores iniciais para os principais de cada parcela.
     */
    private double[] gerarChuteInicial() {
        int n = parametros.quantidadeParcelas();
        double[] chute = new double[n];
        // Inicializa com principal igual ao saldoPrincipal/n
        BigDecimal principalBase = parametros.saldoPrincipal().divide(BigDecimal.valueOf(n), MC);
        for (int i = 0; i < n; i++) {
            chute[i] = principalBase.doubleValue();
        }
        return chute;
    }

    /**
     * Função objetivo para otimização do sistema PRICE.
     * Penaliza desvios nas somas, principais negativos, juros negativos,
     * e garante que os juros sejam decrescentes e o principal crescente.
     *
     * Exemplo de uso:
     * <pre>
     *     ParametrosRedistribuicao parametros = ...;
     *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
     *     double[] principals = {900.0, 950.0, 1000.0};
     *     double penalidade = r.funcaoObjetivo(principals);
     *     // penalidade será 0 se os principais estiverem corretos para o sistema PRICE
     * </pre>
     *
     * @param principals Array de valores de principal para cada parcela.
     * @return Valor da penalidade calculada para o vetor de principais fornecido.
     */
    private double funcaoObjetivo(double[] principals) {
        int n = parametros.quantidadeParcelas();
        BigDecimal[] principalBD = new BigDecimal[n];
        BigDecimal[] jurosBD = new BigDecimal[n];
        BigDecimal valorParcela = parametros.valorParcela();

        for (int i = 0; i < n; i++) {
            principalBD[i] = new BigDecimal(principals[i], MC);
            jurosBD[i] = valorParcela.subtract(principalBD[i],MC);
        }

        BigDecimal somaPrincipals = Arrays.stream(principalBD).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaJuros = Arrays.stream(jurosBD).reduce(BigDecimal.ZERO, BigDecimal::add);

        double penalidade = 0;

        // Restrições de soma
        penalidade += 1e12 * Math.pow(somaPrincipals.subtract(parametros.saldoPrincipal()).doubleValue(), 2);
        penalidade += 1e12 * Math.pow(somaJuros.subtract(parametros.saldoJuros()).doubleValue(), 2);

        // Não-negatividade
        for (int i = 0; i < n; i++) {
            if (principalBD[i].compareTo(BigDecimal.ZERO) < 0)
                penalidade += 1e12 * Math.pow(principalBD[i].doubleValue(), 2);
            if (jurosBD[i].compareTo(BigDecimal.ZERO) < 0)
                penalidade += 1e12 * Math.pow(jurosBD[i].doubleValue(), 2);
        }

        // Price: juros decrescentes, principal crescente
        for (int i = 1; i < n; i++) {
            if (jurosBD[i].compareTo(jurosBD[i-1]) <= 0)
                penalidade += 1e8 * Math.pow(jurosBD[i].subtract(jurosBD[i-1]).doubleValue(), 2);
            if (principalBD[i].compareTo(principalBD[i-1]) >= 0)
                penalidade += 1e8 * Math.pow(principalBD[i-1].subtract(principalBD[i]).doubleValue(), 2);
        }

        return penalidade;
    }

    /**
     * Função objetivo para otimização do sistema de principal variável.
     * Penaliza desvios nas somas, principais negativos, juros negativos,
     * principais não crescentes e principais muito iguais (opcional).
     *
     * Exemplo de uso:
     * <pre>
     *     ParametrosRedistribuicao parametros = ...;
     *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
     *     double[] principals = {1000.0, 1100.0, 1200.0};
     *     double penalidade = r.funcaoObjetivoPrincipalVariavel(principals);
     *     // penalidade será 0 se os principais estiverem corretos para o sistema desejado
     * </pre>
     *
     * @param principals Array de valores de principal para cada parcela.
     * @return Valor da penalidade calculada para o vetor de principais fornecido.
     */
    private double funcaoObjetivoPrincipalVariavel(double[] principals) {
        int n = parametros.quantidadeParcelas();
        BigDecimal[] principalBD = new BigDecimal[n];
        BigDecimal[] jurosBD = new BigDecimal[n];
        BigDecimal valorParcela = parametros.valorParcela();

        for (int i = 0; i < n; i++) {
            principalBD[i] = new BigDecimal(principals[i], MC);
            jurosBD[i] = valorParcela.subtract(principalBD[i]);
        }
        BigDecimal somaPrincipals = Arrays.stream(principalBD).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal somaJuros = Arrays.stream(jurosBD).reduce(BigDecimal.ZERO, BigDecimal::add);

        double penalidade = 0;

        // Restrições de soma
        penalidade += 1e12 * Math.pow(somaPrincipals.subtract(parametros.saldoPrincipal()).doubleValue(), 2);
        penalidade += 1e12 * Math.pow(somaJuros.subtract(parametros.saldoJuros()).doubleValue(), 2);

        // Penalidade para todos os principais iguais (opcional)
        BigDecimal media = somaPrincipals.divide(BigDecimal.valueOf(n), MC);
        for (int i = 0; i < n; i++) {
            penalidade += 1e4 * Math.pow(principalBD[i].subtract(media).doubleValue(), 2);
        }

        // Para principal crescente:
        for (int i = 1; i < n; i++) {
            if (principalBD[i].compareTo(principalBD[i-1]) < 0)
                penalidade += 1e8 * Math.pow(principalBD[i-1].subtract(principalBD[i]).doubleValue(), 2);
        }

        // Não-negatividade
        for (int i = 0; i < n; i++) {
            if (principalBD[i].compareTo(BigDecimal.ZERO) < 0)
                penalidade += 1e12 * Math.pow(principalBD[i].doubleValue(), 2);
            if (jurosBD[i].compareTo(BigDecimal.ZERO) < 0)
                penalidade += 1e12 * Math.pow(jurosBD[i].doubleValue(), 2);
        }

        // Não há penalidade de monotonicidade para principal variável

        return penalidade;
    }

    /**
     * Função objetivo para otimização do sistema SAC (Sistema de Amortização Constante).
     * Penaliza desvios em relação ao principal constante, juros negativos, principais negativos,
     * e diferenças entre a soma principal+juros e o valor da parcela.
     *
     * Exemplo de uso:
     * <pre>
     *     ParametrosRedistribuicao parametros = ...;
     *     RedistribuicaoFinanciamento r = new RedistribuicaoFinanciamento(parametros);
     *     double[] principals = {1000.0, 1000.0, 1000.0};
     *     double penalidade = r.funcaoObjetivoSAC(principals);
     *     // penalidade será 0 se os principais estiverem corretos para SAC
     * </pre>
     *
     * @param principals Array de valores de principal para cada parcela.
     * @return Valor da penalidade calculada para o vetor de principais fornecido.
     */
    private double funcaoObjetivoSAC(double[] principals) {
        int n = parametros.quantidadeParcelas();
        BigDecimal[] principalBD = new BigDecimal[n];
        BigDecimal[] jurosBD = new BigDecimal[n];
        BigDecimal valorParcela = parametros.valorParcela();
        BigDecimal saldoPrincipal = parametros.saldoPrincipal();
        BigDecimal taxa = parametros.taxaJuros();

        // Principal SAC esperado
        BigDecimal principalSAC = saldoPrincipal.divide(BigDecimal.valueOf(n), MC);

        // Calcula juros SAC para cada parcela
        BigDecimal saldoDevedor = saldoPrincipal;
        double penalidade = 0;

        for (int i = 0; i < n; i++) {
            principalBD[i] = new BigDecimal(principals[i], MC);
            // Juros SAC: saldo devedor * taxa
            jurosBD[i] = saldoDevedor.multiply(taxa, MC);
            saldoDevedor = saldoDevedor.subtract(principalSAC, MC);

            // Penalidade para principal diferente do SAC
            penalidade += 1e10 * Math.pow(principalBD[i].subtract(principalSAC).doubleValue(), 2);

            // Penalidade para principal negativo
            if (principalBD[i].compareTo(BigDecimal.ZERO) < 0)
                penalidade += 1e12 * Math.pow(principalBD[i].doubleValue(), 2);

            // Penalidade para juros negativo
            if (jurosBD[i].compareTo(BigDecimal.ZERO) < 0)
                penalidade += 1e12 * Math.pow(jurosBD[i].doubleValue(), 2);

            // Penalidade para principal + juros diferente da parcela
            penalidade += 1e8 * Math.pow(principalBD[i].add(jurosBD[i]).subtract(valorParcela).doubleValue(), 2);
        }

        // Penalidade para soma dos principais diferente do saldo principal
        BigDecimal somaPrincipals = Arrays.stream(principalBD).reduce(BigDecimal.ZERO, BigDecimal::add);
        penalidade += 1e12 * Math.pow(somaPrincipals.subtract(saldoPrincipal).doubleValue(), 2);

        return penalidade;
    }

    /**
     * Adjusts the last installment to ensure that the sum of principals and interest matches the expected balances.
     * This is necessary due to possible rounding errors during the optimization process.
     *
     * @param principals      Array of principal values for each installment.
     * @param juros           Array of interest values for each installment.
     * @param saldoPrincipal  Total expected principal balance.
     * @param saldoJuros      Total expected interest balance.
     * @param valorParcela    Value of each installment.
     */
    private void ajustarUltimaParcela(BigDecimal[] principals, BigDecimal[] juros, BigDecimal saldoPrincipal, BigDecimal saldoJuros, BigDecimal valorParcela) {
        int n = principals.length;
        // Ajusta o último principal para garantir soma exata
        BigDecimal somaPrincipals = Arrays.stream(principals).limit(n-1).reduce(BigDecimal.ZERO, BigDecimal::add);
        principals[n-1] = saldoPrincipal.subtract(somaPrincipals).setScale(2, RoundingMode.HALF_EVEN);
        juros[n-1] = valorParcela.subtract(principals[n-1]).setScale(2, RoundingMode.HALF_EVEN);

        // Ajusta o último juros para garantir soma exata
        BigDecimal somaJuros = Arrays.stream(juros).limit(n-1).reduce(BigDecimal.ZERO, BigDecimal::add);
        juros[n-1] = saldoJuros.subtract(somaJuros).setScale(2, RoundingMode.HALF_EVEN);
        //principals[n-1] = valorParcela.subtract(juros[n-1]).setScale(2, RoundingMode.HALF_EVEN);*/
    }
   /*    public static class ResultadoRedistribuicao {
        private final BigDecimal[] principals;
        private final BigDecimal[] juros;

        public ResultadoRedistribuicao(BigDecimal[] principals, BigDecimal[] juros) {
            this.principals = Arrays.copyOf(principals, principals.length);
            this.juros = Arrays.copyOf(juros, juros.length);
        }

        public BigDecimal[] getPrincipals() {
            return principals;
        }

        public BigDecimal[] getJuros() {
            return juros;
        }

        public void validar(BigDecimal[] parcelas, BigDecimal saldoPrincipal, BigDecimal saldoJuros) {
            // Valida as restrições
            BigDecimal somaPrincipals = BigDecimal.ZERO;
            BigDecimal somaJuros = BigDecimal.ZERO;
            for (BigDecimal p : principals) somaPrincipals = somaPrincipals.add(p);
            for (BigDecimal j : juros) somaJuros = somaJuros.add(j);

            if (somaPrincipals.subtract(saldoPrincipal).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw new IllegalStateException("Soma dos principals não corresponde ao saldo");
            }

            if (somaJuros.subtract(saldoJuros).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw new IllegalStateException("Soma dos juros não corresponde ao saldo");
            }

            for (int i = 0; i < principals.length; i++) {
                if (principals[i].add(juros[i]).subtract(parcelas[i]).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    throw new IllegalStateException("Parcela " + i + " não bate: principal + juros != parcela");
                }
            }
        }
    }*/
}
