package util;
import static io.jenetics.engine.EvolutionResult.toBestPhenotype;
import static io.jenetics.engine.Limits.bySteadyFitness;

import core.SimulationMain;
import io.jenetics.*;
import io.jenetics.engine.Codecs;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.util.DoubleRange;
import io.jenetics.util.Factory;

public class GA {
    //适应度函数
//    private static double fitness(double[] r) {
//        return SimulationMain.simulate(r);
//    }


//    public static void main(String[] args) {
//        Engine<DoubleGene, Double> engine = Engine.builder(
//                GA::fitness, Codecs.ofVector(DoubleRange.of(0, 1), 6))
//                .populationSize(500)
//                .optimize(Optimize.MINIMUM)
//                .alterers(
//                        new Mutator<>(0.55),
//                        new SinglePointCrossover<>(0.06))
//                .build();
//        EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();
//        Phenotype<DoubleGene, Double> best = engine.stream()
//                .limit(bySteadyFitness(7))
//                .peek(statistics)
//                .collect(toBestPhenotype());
//        System.out.println(statistics);
//        System.out.println(best);
//    }

}
