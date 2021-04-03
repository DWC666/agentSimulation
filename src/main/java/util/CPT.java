package util;

import core.SimulationMain;
import entity.Link;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.List;

/**
 * 累积前景理论相关函数
 */
public class CPT {
    /*累积前景理论相关参数*/
    private static final double cpt_alpha = 0.89; //价值函数中，收益时对应风险偏好系数。取值参考“基于前景理论的居民出行方式选择_张薇”
    private static final double cpt_beta = 0.92; //价值函数中，损失时对应风险偏好系数
    private static final double cpt_lambda = 2.25; //价值函数中，损失时对应损失厌恶系数
    private static final double cpt_gamma = 0.61; //概率权重函数中，收益时对应参数
    private static final double cpt_delta = 0.69; //概率权重函数中，损失时对应参数
    public static final double cpt_acceptableFactor = 1.2; //可接受系数。该系数与最低成本的乘积作为CPT参考点
    public static final double cpt_highIncomeTimeWeight = 0.6; //高收入人群综合cpv中时间权重
    public static final double cpt_highIncomeCurrencyWeight = 0.4; //高收入人群综合cpv中货币权重
    public static final double cpt_lowIncomeTimeWeight = 0.4; // 低收入人群综合cpv中时间权重
    public static final double cpt_lowIncomeCurrencyWeight = 0.6; //低收入人群综合cpv中货币权重
    public static double car_standardDeviationRate = 0.4; //小汽车路段的行程时间的正态分布中，标准差与均值的比值
    public static double notCar_standardDeviationRate = 0.3; //非小汽车路段的行程时间的正态分布中，标准差与均值的比值


    /**
     * 返回超路径的正态分布
     * @return 均值 和 标准差
     */
    public static NormalDistribution hyperPathNormalDistribution(List<Integer> path){
        double average = 0.0;
        double variance = 0.0;
        for(int i=0; i<path.size()-1; i++){
            Link link = SimulationMain.getLink(path.get(i), path.get(i + 1));
            if(link.getTrafficMode() == TrafficMode.CAR){
                average += link.getTime();
                variance += Math.pow(link.getTime() * car_standardDeviationRate, 2);
            }else{
                average += link.getTime();
                variance += Math.pow(link.getTime() * notCar_standardDeviationRate, 2);
            }
        }
        //将时间乘以-1转换为效用
        return new NormalDistribution(-1 * average, Math.pow(variance, 0.5));
    }


    /**
     * 累积前景理论的概率权重函数
     * @param profit 是否收益
     * @param p 效用值对应的客观概率
     * @return 概率权重（主观概率）
     */
    private static double probabilityWeightFunction(boolean profit, double p){
        if(profit){
            return Math.pow(p, cpt_gamma) / Math.pow(Math.pow(p, cpt_gamma) + Math.pow(1-p, cpt_gamma), 1.0/cpt_gamma);
        }else{
            return Math.pow(p, cpt_delta) / Math.pow(Math.pow(p, cpt_delta) + Math.pow(1-p, cpt_delta), 1.0/cpt_delta);
        }
    }


    /**
     * 累积前景理论的价值函数
     * @param difference 实际值 减  参考点
     * @return
     */
    private static double valueFunction(double difference){
        if(difference >= 0){ //收益时
            return Math.pow(difference, cpt_alpha);
        }else{ //亏损时
            return -1 * cpt_lambda * Math.pow(-difference, cpt_beta);
        }
    }


    /**
     * 累积主观概率函数
     * @param value
     * @param step
     * @param normalDistribution
     * @param profit
     * @return
     */
    private static double cumulativeProbability(double value, double step, NormalDistribution normalDistribution, boolean profit){
        double cp = 0.0;
        if(profit){
            double p1 = 1 - normalDistribution.cumulativeProbability(value);
            double p2 = 1 - normalDistribution.cumulativeProbability(value + step);
            cp = probabilityWeightFunction(profit, p1) - probabilityWeightFunction(profit, p2);
        }else{
            double p1 = normalDistribution.cumulativeProbability(value);
            double p2 = normalDistribution.cumulativeProbability(value - step);
            cp = probabilityWeightFunction(profit, p1) - probabilityWeightFunction(profit, p2);
        }
        return cp;
    }



    /**
     * 计算累积前景值
     * @param referencePoint 参考点
     * @param normalDistribution 正态分布
     * @return
     */
    public static double cpv(double referencePoint, NormalDistribution normalDistribution){
        double cpv = 0.0;
        int n = 1000;
        double upperLimit = normalDistribution.getMean() + 20 * normalDistribution.getStandardDeviation();
        double lowerLimit = normalDistribution.getMean() - 20 * normalDistribution.getStandardDeviation();
        double step = (upperLimit - lowerLimit) / 1000;
        for(double v = referencePoint; v <= upperLimit; v+=step){
            cpv += cumulativeProbability(v, step, normalDistribution,true) * valueFunction(v - referencePoint);
        }
        for(double v = referencePoint; v >= lowerLimit; v-=step){
            cpv += cumulativeProbability(v, step, normalDistribution,false) * valueFunction(v - referencePoint);
        }
        return cpv;
    }


    /**
     * 计算定值的累积前景值
     * @param referencePoint 参考点
     * @param value 定值
     * @return
     */
    public static double cpv4FixedValue(double referencePoint, double value){
        double cpv = 0.0;
        boolean profit = referencePoint - value >= 0 ? true : false;
        cpv = probabilityWeightFunction(profit, 1) * valueFunction(value - referencePoint);
        return cpv;
    }


    public static void main(String[] args) {
        NormalDistribution normalDistribution = new NormalDistribution(30, 2);
        for(int i=1; i<10; i++){
            System.out.println(-30+i*4);
            System.out.println("a: " + cpv(-30+i*4, new NormalDistribution(-30, 2)));
            System.out.println("b: " + cpv(-30+i*4, new NormalDistribution(-30, 6)));
//            System.out.println("a: " + cpv4FixedValue(-30+i*4, -30));
//            System.out.println("b: " + cpv4FixedValue(-30+i*4, -20));

        }


    }

}
