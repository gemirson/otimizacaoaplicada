package org.com.pangolin;

import org.com.pangolin.redistribuicao.RedistribuicaoFinanciamento;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        double[] parcelas = {600, 600, 600, 600};
        double saldoPrincipal = 1600;
        double saldoJuros = 600;

        // Com principal constante
        RedistribuicaoFinanciamento rfConstante = new RedistribuicaoFinanciamento(
                parcelas, saldoPrincipal, saldoJuros, true);
        RedistribuicaoFinanciamento.ResultadoRedistribuicao resultadoConstante = rfConstante.redistribuir();

        resultadoConstante.validar(parcelas, saldoPrincipal, saldoJuros);

        System.out.println("Redistribuição com Principal Constante:");
        for (int i = 0; i < resultadoConstante.getPrincipals().length; i++) {
            System.out.printf("Parcela %d: Principal=%.2f, Juros=%.2f, Total=%.2f%n",
                    i+1, resultadoConstante.getPrincipals()[i],
                    resultadoConstante.getJuros()[i],
                    resultadoConstante.getPrincipals()[i] + resultadoConstante.getJuros()[i]);
        }

        // Sem principal constante (comportamento original)
        RedistribuicaoFinanciamento rfVariavel = new RedistribuicaoFinanciamento(
                parcelas, saldoPrincipal, saldoJuros, false);
        RedistribuicaoFinanciamento.ResultadoRedistribuicao resultadoVariavel = rfVariavel.redistribuir();

        resultadoVariavel.validar(parcelas, saldoPrincipal, saldoJuros);
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        //System.out.printf("Hello and welcome!");

       /* for (int i = 1; i <= 5; i++) {
            //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
            System.out.println("i = " + i);
        }*/
    }
}