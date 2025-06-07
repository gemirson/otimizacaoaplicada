package org.com.pangolin.redistribuicao;

import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.optimization.ConvergenceChecker;
import org.apache.commons.math3.optimization.SimpleValueChecker;

import java.util.Arrays;

public class RedistribuicaoFinanciamento {

        private final double[] parcelas;
        private final double saldoPrincipal;
        private final double saldoJuros;
        private final int numParcelas;
        private final boolean principalConstante;

        public RedistribuicaoFinanciamento(double[] parcelas, double saldoPrincipal,
                                           double saldoJuros, boolean principalConstante) {
            this.parcelas = Arrays.copyOf(parcelas, parcelas.length);
            this.saldoPrincipal = saldoPrincipal;
            this.saldoJuros = saldoJuros;
            this.numParcelas = parcelas.length;
            this.principalConstante = principalConstante;

            validarDadosIniciais();
        }

        private void validarDadosIniciais() {
            if (saldoPrincipal < 0 || saldoJuros < 0) {
                throw new IllegalArgumentException("Valores de principal e juros devem ser não-negativos");
            }

            if (principalConstante && saldoPrincipal > 0 && numParcelas > 0) {
                double principalPorParcela = saldoPrincipal / numParcelas;
                for (double parcela : parcelas) {
                    if (principalPorParcela > parcela) {
                        throw new IllegalArgumentException(
                                "Principal constante por parcela (" + principalPorParcela +
                                        ") excede o valor de uma parcela");
                    }
                }
            }
        }

        public ResultadoRedistribuicao redistribuir() {
            if (principalConstante) {
                return redistribuirComPrincipalConstante();
            } else {
                return redistribuirSemPrincipalConstante();
            }
        }

        private ResultadoRedistribuicao redistribuirComPrincipalConstante() {
            double[] principals = new double[numParcelas];
            double principalPorParcela = saldoPrincipal / numParcelas;
            Arrays.fill(principals, principalPorParcela);

            // Otimiza apenas a distribuição de juros
            SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
            NelderMeadSimplex simplex = new NelderMeadSimplex(numParcelas);

            ObjectiveFunction function = new ObjectiveFunction(
                    x -> calcularFuncaoObjetivoJuros(x, principals));

            PointValuePair solution = optimizer.optimize(
                    new MaxEval(100000),
                    function,
                    GoalType.MINIMIZE,
                    simplex,
                    new InitialGuess(criarChuteInicialJuros()),
                    new NonNegativeConstraint(true)
            );

            double[] juros = solution.getPoint();
            return new ResultadoRedistribuicao(principals, juros);
        }

        private double calcularFuncaoObjetivoJuros(double[] juros, double[] principals) {
            double penalidade = 0;

            // Restrição: juros <= valor da parcela - principal
            for (int i = 0; i < numParcelas; i++) {
                double maxJuros = parcelas[i] - principals[i];
                if (juros[i] > maxJuros) {
                    penalidade += Math.pow(juros[i] - maxJuros, 2) * 1000;
                }
            }

            // Restrição: soma dos juros = saldoJuros
            double somaJuros = Arrays.stream(juros).sum();
            penalidade += Math.pow(somaJuros - saldoJuros, 2);

            // Objetivo: juros decrescentes
            for (int i = 1; i < numParcelas; i++) {
                if (juros[i] > juros[i-1]) {
                    penalidade += Math.pow(juros[i] - juros[i-1], 2);
                }
            }

            return penalidade;
        }

        private double[] criarChuteInicialJuros() {
            double[] chute = new double[numParcelas];
            double totalJuros = 0;
            double[] pesos = new double[numParcelas];

            for (int i = 0; i < numParcelas; i++) {
                pesos[i] = Math.exp(-0.5 * i);
                totalJuros += pesos[i];
            }

            for (int i = 0; i < numParcelas; i++) {
                double juroCalculado = (pesos[i] / totalJuros) * saldoJuros;
                double maxJuros = parcelas[i] - (saldoPrincipal / numParcelas);
                chute[i] = Math.min(juroCalculado, maxJuros);
            }

            return chute;
        }
    private double calcularFuncaoObjetivo(double[] x) {
        double[] principals = Arrays.copyOfRange(x, 0, numParcelas);
        double[] juros = Arrays.copyOfRange(x, numParcelas, 2 * numParcelas);

        double penalidade = 0;

        // Restrição 1: principal >= 0 (já garantido pelo NonNegativeConstraint)
        // Restrição 2: juros <= valor da parcela
        for (int i = 0; i < numParcelas; i++) {
            if (juros[i] > parcelas[i]) {
                penalidade += Math.pow(juros[i] - parcelas[i], 2) * 1000; // Peso alto para esta restrição
            }
        }

        // Restrição 3: principal + juros <= valor da parcela
        for (int i = 0; i < numParcelas; i++) {
            double totalParcela = principals[i] + juros[i];
            if (totalParcela > parcelas[i]) {
                penalidade += Math.pow(totalParcela - parcelas[i], 2) * 1000;
            }
        }

        // Restrição 4: soma dos principals = saldoPrincipal
        double somaPrincipals = Arrays.stream(principals).sum();
        penalidade += Math.pow(somaPrincipals - saldoPrincipal, 2);

        // Restrição 5: soma dos juros = saldoJuros
        double somaJuros = Arrays.stream(juros).sum();
        penalidade += Math.pow(somaJuros - saldoJuros, 2);

        // Objetivo: juros decrescentes (maior no início)
        double decaimento = 0;
        for (int i = 1; i < numParcelas; i++) {
            if (juros[i] > juros[i-1]) {
                decaimento += Math.pow(juros[i] - juros[i-1], 2);
            }
        }

        return penalidade + decaimento;
    }
    private ResultadoRedistribuicao redistribuirSemPrincipalConstante() {
        // Configuração do otimizador para o caso de principal variável
        SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
        NelderMeadSimplex simplex = new NelderMeadSimplex(numParcelas * 2);

        // Função objetivo com todas as restrições
        ObjectiveFunction function = new ObjectiveFunction(this::calcularFuncaoObjetivoCompleto);

        // Otimização considerando todas as variáveis (principal e juros)
        PointValuePair solution = optimizer.optimize(
                new MaxEval(100000),
                function,
                GoalType.MINIMIZE,
                simplex,
                new InitialGuess(criarChuteInicialCompleto()),
                new NonNegativeConstraint(true)
        );

        double[] valores = solution.getPoint();
        double[] principals = Arrays.copyOfRange(valores, 0, numParcelas);
        double[] juros = Arrays.copyOfRange(valores, numParcelas, 2 * numParcelas);

        return new ResultadoRedistribuicao(principals, juros);
    }

    private double calcularFuncaoObjetivoCompleto(double[] x) {
        double[] principals = Arrays.copyOfRange(x, 0, numParcelas);
        double[] juros = Arrays.copyOfRange(x, numParcelas, 2 * numParcelas);

        double penalidade = 0;

        // 1. Restrição: principal + juros <= valor da parcela
        for (int i = 0; i < numParcelas; i++) {
            double totalParcela = principals[i] + juros[i];
            if (totalParcela > parcelas[i]) {
                penalidade += Math.pow(totalParcela - parcelas[i], 2) * 1000;
            }
        }

        // 2. Restrição: soma dos principals = saldoPrincipal
        double somaPrincipals = Arrays.stream(principals).sum();
        penalidade += Math.pow(somaPrincipals - saldoPrincipal, 2);

        // 3. Restrição: soma dos juros = saldoJuros
        double somaJuros = Arrays.stream(juros).sum();
        penalidade += Math.pow(somaJuros - saldoJuros, 2);

        // 4. Objetivo: juros decrescentes (maior no início)
        double decaimentoJuros = 0;
        for (int i = 1; i < numParcelas; i++) {
            if (juros[i] > juros[i-1]) {
                decaimentoJuros += Math.pow(juros[i] - juros[i-1], 2);
            }
        }

        // 5. Objetivo: principals crescentes ou constantes (menor no início)
        double suavidadePrincipal = 0;
        for (int i = 1; i < numParcelas; i++) {
            if (principals[i] < principals[i-1]) {
                suavidadePrincipal += Math.pow(principals[i-1] - principals[i], 2);
            }
        }

        return penalidade + decaimentoJuros + suavidadePrincipal * 0.5;
    }

    private double[] criarChuteInicialCompleto() {
        double[] chute = new double[numParcelas * 2];

        // Distribuição inicial do principal (crescente)
        double principalBase = saldoPrincipal / (numParcelas * 1.5);
        for (int i = 0; i < numParcelas; i++) {
            chute[i] = principalBase * (1 + 0.5 * i / numParcelas);
        }

        // Distribuição inicial dos juros (decrescente exponencial)
        double totalJuros = 0;
        double[] pesosJuros = new double[numParcelas];
        for (int i = 0; i < numParcelas; i++) {
            pesosJuros[i] = Math.exp(-0.8 * i);
            totalJuros += pesosJuros[i];
        }

        for (int i = 0; i < numParcelas; i++) {
            double juroCalculado = (pesosJuros[i] / totalJuros) * saldoJuros;
            // Garante que juros não ultrapassem o valor da parcela - principal
            double maxJuros = parcelas[i] - chute[i];
            chute[numParcelas + i] = Math.min(juroCalculado, Math.max(0, maxJuros));
        }

        // Ajuste fino para garantir somas totais
        ajustarSomas(chute);

        return chute;
    }

    private void ajustarSomas(double[] chute) {
        // Ajusta para garantir que as somas batam exatamente com os saldos
        double[] principals = Arrays.copyOfRange(chute, 0, numParcelas);
        double[] juros = Arrays.copyOfRange(chute, numParcelas, 2 * numParcelas);

        // Fator de correção para principals
        double somaPrincipals = Arrays.stream(principals).sum();
        if (somaPrincipals > 0) {
            double fatorPrincipal = saldoPrincipal / somaPrincipals;
            for (int i = 0; i < numParcelas; i++) {
                chute[i] *= fatorPrincipal;
            }
        }

        // Fator de correção para juros
        double somaJuros = Arrays.stream(juros).sum();
        if (somaJuros > 0) {
            double fatorJuros = saldoJuros / somaJuros;
            for (int i = 0; i < numParcelas; i++) {
                chute[numParcelas + i] *= fatorJuros;
            }
        }

        // Garante que não ultrapasse os limites das parcelas após ajuste
        for (int i = 0; i < numParcelas; i++) {
            double principal = chute[i];
            double juro = chute[numParcelas + i];
            double maxJuro = parcelas[i] - principal;

            if (juro > maxJuro) {
                chute[numParcelas + i] = maxJuro;
            }
        }
    }
    public static class ResultadoRedistribuicao {
        private final double[] principals;
        private final double[] juros;

        public ResultadoRedistribuicao(double[] principals, double[] juros) {
            this.principals = Arrays.copyOf(principals, principals.length);
            this.juros = Arrays.copyOf(juros, juros.length);
        }

        public double[] getPrincipals() {
            return principals;
        }

        public double[] getJuros() {
            return juros;
        }

        public void validar(double[] parcelas, double saldoPrincipal, double saldoJuros) {
            // Valida as restrições
            double somaPrincipals = Arrays.stream(principals).sum();
            double somaJuros = Arrays.stream(juros).sum();

            if (Math.abs(somaPrincipals - saldoPrincipal) > 1e-6) {
                throw new IllegalStateException("Soma dos principals não corresponde ao saldo");
            }

            if (Math.abs(somaJuros - saldoJuros) > 1e-6) {
                throw new IllegalStateException("Soma dos juros não corresponde ao saldo");
            }

            for (int i = 0; i < principals.length; i++) {
                if (principals[i] + juros[i] > parcelas[i] + 1e-6) {
                    throw new IllegalStateException("Parcela " + i + " excede o valor permitido");
                }
            }
        }
    }
}
