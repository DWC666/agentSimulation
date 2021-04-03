package ga;

import core.SimulationMain;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

import java.util.ArrayList;
import java.util.List;

public class MyProblem extends AbstractDoubleProblem {
    public MyProblem(){
        this(6);
    }

    public MyProblem(Integer numberOfVariables){
        setNumberOfVariables(numberOfVariables);//设定决策变量个数
        setNumberOfObjectives(1);//设定优化目标函数个数
        setName("dwc");//给这个问题起名

        List<Double> lowerLimit = new ArrayList<>(getNumberOfVariables()) ;
        List<Double> upperLimit = new ArrayList<>(getNumberOfVariables()) ;

        //设置定义域
        for (int i = 0; i < getNumberOfVariables(); i++) {
            lowerLimit.add(0.0);
            upperLimit.add(1.0);
        }

        setVariableBounds(lowerLimit, upperLimit);
    }

    //这里就是优化目标函数的实现过程，Algorithm.evlataionPopulation()会调用这个方法
    @Override
    public void evaluate(DoubleSolution solution) {
        int numberOfVariables = solution.getNumberOfVariables();
        double[] rate = new double[numberOfVariables];
        for(int i=0; i<numberOfVariables; i++){
            rate[i] = solution.getVariable(i);
        }
//        solution.setObjective(0, -1*SimulationMain.simulate(rate));
    }
}
