package util;

import core.SimulationMain;
import jxl.write.WriteException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/*
常量工具类
*/
public class MyUtils {
    /*
    下面矩阵的值为路段编号：
            0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15  16  17  18  19  20  21  22  23  24
            ———————————————————————————————————————————————————————————————————————————————————————————————————
        0 | -   -   -   -   -   -	46	-	-   -   -   -	-   -   -   -   -   -   -   0   -   -   -   -   -
        1 | -   -   -   -   10  -	-	50	-   -   -   -	-   -   -   -   -   -   -   3   4   -   -   -   -
        2 | -   -   -   8   -   -	-	-	54  -   -   -	-   -   -   -   -   -   -   -   7   -   -   -   -
        3 | -   -   9   -   -   -	-	-	-   56  -   -	-   -   -   -   -   -   -   -   -   12  -   -   -
        4 | -   11  -   -   -   16	-	-	-   -   58  -	-   -   -   -   -   -   -   -   -   -   -   -   -
        5 | -   -   -   -   17  -	-	-	-   -   -   60	-   -   -   -   -   -   -   -   -   15  -   -   -
        6 | 47  -   -   -   -   -	-	-	-   -   -   -	-   -   -   -   -   -   -   -   -   -   18  -   -
        7 | -   51  -   -   -   -	-	-	-   -   28  -	64  -   -   -   74  -   -   -   -   -   21  22  -
        8 | -   -   55  -   -   -	-	-	-   26  -   -	-   66  -   -   -   -   -   -   -   -   -   25  -
        9 | -   -   -   57  -   -	-	-	27  -   -   -	-   -   68  -   -   -   -   -   -   -   -   -   30
        10| -   -   -   -   59  -	-	29	-   -   -   34	-   -   -   -   -   73  -   -   -   -   -   -   -
        11| -   -   -   -   -   61	-	-	-   -   35  -	-   -   -   70  -   -   76  -   -   -   -   -   33
        12| -   -   -   -   -   -	-	65	-   -   -   -	-   36  -   -   -   -   -   -   -   -   -   -   -
        13| -   -   -   -   -   -	-	-	67  -   -   -	37  -   38  -   -   -   -   -   -   -   -   -   -
        14| -   -   -   -   -   -	-	-	-   69  -   -	-   39  -   40  -   -   -   -   -   -   -   -   -
        15| -   -   -   -   -   -	-	-	-   -   -   71	-   -   41  -   -   -   -   -   -   -   -   -   -
        16| -   -   -   -   -   -	-	75	-   -   -   -	-   -   -   -   -   42  -   -   -   -   -   -   -
        17| -   -   -   -   -   -	-	-	-   -   72  -	-   -   -   -   43  -   44  -   -   -   -   -   -
        18| -   -   -   -   -   -	-	-	-   -   -   77	-   -   -   -   -   45  -   -   -   -   -   -   -
        19| 1   2   -   -   -   -	-	-	-   -   -   -	-   -   -   -   -   -   -   -   -   -   48  -   -
        20| -   5   6   -   -   -	-	-	-   -   -   -	-   -   -   -   -   -   -   -   -   -   -   52  -
        21| -   -   -   13  -   14	-	-	-   -   -   -	-   -   -   -   -   -   -   -   -   -   -   -   62
        22| -   -   -   -   -   -	19	20	-   -   -   -	-   -   -   -   -   -   -   49  -   -   -   -   -
        23| -   -   -   -   -   -	-	23	24  -   -   -	-   -   -   -   -   -   -   -   53  -   -   -   -
        24| -   -   -   -   -   -	-	-	-   31  -   32	-   -   -   -   -   -   -   -   -   63  -   -   -

    */

    public static final double walkSpeed = 1.2; //步行速度（米/秒）
    public static final double metroSpeed = 12; //地铁速度（米/秒）
    public static final double busSpeed = 6.9; //25公里/小时，公交在专用道的速度（米/秒）

    //基本OD加载（辆/小时）,
    public static final int[][] baseOD = {
            {120, 120},
            {120, 120},
            {120, 120}
    };



    /**
     * 打印矩阵
     * @param matrix
     */
    public static void printMatrix(double[][] matrix){
        System.out.println("\n打印矩阵： ");
        for(double[] row : matrix){
            System.out.println(Arrays.toString(row));
        }
    }

    /**
     * 打印矩阵
     * @param matrix
     */
    public static void printMatrix(int[][] matrix){
        System.out.println("\n打印矩阵： ");
        for(int[] row : matrix){
            System.out.println(Arrays.toString(row));
        }
    }


    /**
     * 放大OD
     * @param baseOD
     * @param n
     * @return
     */
    private static int[][] addOD(int[][] baseOD, double n){
        int rows = baseOD.length;
        int cols = baseOD[0].length;
        int[][] OD = new int[rows][cols];
        for(int i=0; i<rows; i++) {
            for (int j = 0; j < cols; j++) {
                OD[i][j] = (int) (baseOD[i][j] * n);
            }
        }
        return OD;
    }

    /**
     * 通过新的拟合函数返回动态OD
     * @param baseOD
     * @param time
     * @return
     */
    public static double[][] getDynamicOD2(int[][] baseOD, int time){
        baseOD = addOD(baseOD, 1.8);
        int rows = baseOD.length;
        int cols = baseOD[0].length;
        double[][] result = new double[rows][cols];
        double factor;
        if(time <= 227){
            factor =  0.00258 * time + 0.221;
        }else if(time <= 1145){
            factor = 0.2 * Math.sin(time * Math.PI / 300 - 2.32) + 0.8;
        }else{
            factor = -0.00171 * time + 2.7048;
        }
        for(int i=0; i<rows; i++){
            for(int j=0; j<cols; j++){
//                result[i][j] = baseOD[i][j] * factor / 60;
                result[i][j] = baseOD[i][j] * factor / (3600 / SimulationMain.stepSize);
            }
        }
        return result;
    }



    /**
     * 正态分布函数
     * @param mean  均值
     * @param standardDeviation 标准差
     * @return 符合上述参数的正态分布随机值
     */
    public static double normallyDistribution(double mean, double standardDeviation){
        Random random = new Random();
        return (random.nextGaussian() * standardDeviation + mean);
    }


    /**
     * 以给定的概率（0-100）返回true
     * @param numUnderHundred
     * @return
     */
    public static boolean fixedProbabilityReturnTrue(double numUnderHundred){
        if(numUnderHundred < 0){
            throw new RuntimeException("参数值只能为0到100");
        }
        Random random = new Random();
        double randomNum = random.nextDouble() * 100;
        if(randomNum <= numUnderHundred){
            return true;
        }else{
            return false;
        }
    }


    /**
     * 矩阵各元素之和
     * @param matrix
     * @return
     */
    public static int matrixSum(double[][] matrix){
        int sum = 0;
        int rows = matrix.length;
        int cols = matrix[0].length;
        for(int i=0; i<rows; i++){
            for(int j=0; j<cols; j++){
                sum += Math.ceil(matrix[i][j]);
            }
        }
        return sum;
    }


    public static double fun(int time){
        double factor;
        if(time <= 227){
            factor =  0.00258 * time + 0.221;
        }else if(time <= 1145){
            factor = 0.2 * Math.sin(time * Math.PI / 300 - 2.32) + 0.8;
        }else{
            factor = -0.00171 * time + 2.7048;
        }
        return factor;
    }


    public static void main(String[] args) throws IOException, WriteException {

        for(int i=0; i<=100; i++){
            System.out.println(fixedProbabilityReturnTrue(50));
        }

    }
}
