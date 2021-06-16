package util;

import core.SimulationMain;

import java.util.*;

public class DijkstraAlgorithm {
    //不能设置为Integer.MAX_VALUE，否则两个Integer.MAX_VALUE相加会溢出导致出现负权
    public static int MAX_VALUE = 99999999;

    public static HashSet<Integer> metroNodes = new HashSet<>(Arrays.asList(new Integer[]{6, 7, 8}));

    /*
            0   1   2   3   4   5   6   7   8   9   10
            ———————————————————————————————————————————
        0 | 0   2   -   -   -   2   -   -   -   -   -
        1 | 2   0   2   -   -   -   0   -   -   -   -
        2 | -   2   0   2   -   -   -   1   -   -   -
        3 | -   -   2   0   4   3   -   -   1   -   4
        4 | -   -   -   4   0   -   -   -   -   4   4
        5 | 2   -   -   3   -   -   -   -   -   -   -
        6 | -   1   -   -   -   -   0   1   -   -   -
        7 | -   -   1   -   -   -   1   0   1   -   -
        8 | -   -   -   0   -   -   -   1   0   -   -
        9 | -   -   -   -   4   -   -   -   -   -   -
        10| -   -   -   4   4   -   -   -   -   -   -
        */
    static int[][] matrix = {
            {0, 2, MAX_VALUE, MAX_VALUE, MAX_VALUE, 2, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE},
            {2, 0, 2, MAX_VALUE, MAX_VALUE, MAX_VALUE, 0, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE},
            {MAX_VALUE, 2, 0, 2, MAX_VALUE, MAX_VALUE, MAX_VALUE, 1, MAX_VALUE, MAX_VALUE, MAX_VALUE},
            {MAX_VALUE, MAX_VALUE, 2, 0, 4, 3, MAX_VALUE, MAX_VALUE, 1, MAX_VALUE, 4},
            {MAX_VALUE, MAX_VALUE, MAX_VALUE, 4, 0, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, 4, 4},
            {2, MAX_VALUE, MAX_VALUE, 3, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE},
            {MAX_VALUE, 1, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, 0, 1, MAX_VALUE, MAX_VALUE, MAX_VALUE},
            {MAX_VALUE, MAX_VALUE, 1, MAX_VALUE, MAX_VALUE, MAX_VALUE, 1, 0, 1, MAX_VALUE, MAX_VALUE},
            {MAX_VALUE, MAX_VALUE, MAX_VALUE, 0, MAX_VALUE, MAX_VALUE, MAX_VALUE, 1, 0, MAX_VALUE, MAX_VALUE},
            {MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, 4, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE},
            {MAX_VALUE, MAX_VALUE, MAX_VALUE, 4, 4, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE, MAX_VALUE}
    };

    //节点之间的路段类型： 0 —— 没有路段， 1 —— 汽车路段， 2 —— 公共交通路段（地铁、公交） 3 —— 慢行路段  4 —— 转换路段
    static int[][] type = {
            {0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0},
            {1, 0, 1, 0, 0, 0, 4, 0, 0, 0, 0},
            {0, 1, 0, 1, 0, 0, 0, 4, 0, 0, 0},
            {0, 0, 1, 0, 1, 1, 0, 0, 4, 0, 3},
            {0, 0, 0, 1, 0, 0, 0, 0, 0, 3, 3},
            {1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 4, 0, 0, 0, 0, 0, 2, 0, 0, 0},
            {0, 0, 4, 0, 0, 0, 2, 0, 2, 0, 0},
            {0, 0, 0, 4, 0, 0, 0, 2, 0, 0, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 3, 3, 0, 0, 0, 0, 0, 0}
    };


    public static class DijkstraResult<T>{
        public T cost;
        public List<Integer> path;

        public DijkstraResult(){};

        public DijkstraResult(T cost, List<Integer> path){
            this.cost = cost;
            this.path = path;
        }
    }


    public static void main(String[] args) {
        //调用dijstra算法计算最短路径
        DijkstraResult dijkstraResult = newDijkstra(matrix, 0, 4);
//        DijkstraResult<Double> dijkstraResult = dijkstra(MyUtils.linkLengthMatrix, 19, 19, null);
        System.out.println("最短路：" + dijkstraResult.path + " 距离为： " + dijkstraResult.cost);
    }



    /**
     * 从前继节点列表查找从起点到终点的路径
     * @param preNode 前继节点列表
     * @param endNode 终点
     * @return
     */
    private static List<Integer> getPath(int[] preNode, int endNode){
        LinkedList<Integer> path = new LinkedList<>();
        int pre = endNode;
        while(pre != -1){
            path.addFirst(pre);
            pre = preNode[pre];
        }
        return path;
    }


    /**
     * 将a到b的最短路径与 b到c的最短路径合拼，删除重复节点，返回合并后的路径
     * @param path1
     * @param path2
     * @return
     */
    public static List<Integer> mergePath(List<Integer> path1, List<Integer> path2){
        if(path1 == null || path1.size() <= 1){
            return path2;
        }
        if(path2 == null || path2.size() <= 1){
            return path1;
        }
        if(path1.get(0) == path2.get(0)){
            return path2;
        }
        List<Integer> mergedPath = new LinkedList<>();
        mergedPath.addAll(path1);
        mergedPath.remove(path1.size() - 1);
        mergedPath.addAll(path2);
        return mergedPath;
    }


    /**
     * 判断路径中是否包含公共交通节点
     * @param path
     * @return
     */
    public static boolean takePublicTraffic(List<Integer> path){
        for(int i : path){
            if(SimulationMain.publicTrafficNodes.contains(i)){
                return true;
            }
        }
        return false;
    }


    /**
     * 寻找停车换乘公共交通方案中的最短路
     * @param matrix 成本矩阵
     * @param from 起点
     * @param midNode 停车节点
     * @param to 终点
     * @return
     */
    public static DijkstraResult<Double> dijkstra4ParkAndRide(double[][] matrix, int from, int midNode, int to){
        DijkstraResult<Double> dijkstraResult1 = dijkstra(matrix, from, midNode, SimulationMain.publicTrafficNodes);
        DijkstraResult<Double> dijkstraResult2 = dijkstraAfterPark(matrix, midNode, to);
        //判断是否乘坐了公共交通，避免停车后自接步行前往目的地
//        boolean takePublicTraffic = takePublicTraffic(dijkstraResult2.path);
//        double totalCost = Double.MAX_VALUE;
//        if(takePublicTraffic){
//            totalCost = dijkstraResult1.cost + dijkstraResult2.cost;
//        }
        List<Integer> mergePath = mergePath(dijkstraResult1.path, dijkstraResult2.path);
        return new DijkstraResult<Double>(dijkstraResult1.cost + dijkstraResult2.cost, mergePath);
    }

    /**
     *
     * @param matrix 成本矩阵
     * @param from 起点
     * @param to 终点
     * @param excludedNodes 寻找最短路径时不考虑的节点
     * @return
     */
    public static DijkstraResult<Double> dijkstra(double[][] matrix, int from, int to, Set<Integer> excludedNodes) {
        int len = matrix.length;

        if(excludedNodes == null){
            excludedNodes = new HashSet<>();
        }
        if(from >= len || to >= len || from == to){
            throw new RuntimeException("异常：起始点" + from + "与终点" + to + "相同 或 起始点（终点）超出范围 ！");
        }
        if(!excludedNodes.isEmpty() && excludedNodes.contains(to)){
            throw new RuntimeException("异常：终点" + to + "在排除考虑的数组中！");
        }

        Set<Integer> visited = new HashSet<>(); //已经求出最短路的节点
        Set<Integer> notVisited = new HashSet<>(); //没有求出最短路的节点
        int[] preNode = new int[len]; //前驱节点列表
        double[] length = new double[len]; //出发点到各点的最短距离

        for (int i = 0; i < len; i++) {
            preNode[i] = -1;
            length[i] = MAX_VALUE;
            if (i != from) {
                notVisited.add(i);
            }
        }

        visited.add(from);
        length[from] = 0;
        int midNode = from;

        while(!visited.contains(to)){
            for (int node : notVisited) {
                if (!excludedNodes.contains(node)) { //如果当前节点不在被排除的节点中
                    if (length[midNode] + matrix[midNode][node] < length[node]) {
                        length[node] = length[midNode] + matrix[midNode][node];
                        preNode[node] = midNode;
                    }
                }
            }

            double min = MAX_VALUE;
            for (int j : notVisited) {
                if (length[j] < min) {
                    min = length[j];
                    midNode = j;
                }
            }

            visited.add(midNode);
            notVisited.remove(midNode);
        }

        return new DijkstraResult<Double>(length[to], getPath(preNode, to));
    }

    /**
     *
     * @param matrix 邻接矩阵
     * @param from 起点
     * @param to 终点
     * @param excludedNodes 寻找最短路径时不考虑的节点
     * @return
     */
//    public static DijkstraResult<Double> dijkstra(double[][] matrix, int from, int to, Set<Integer> excludedNodes) {
//        int len = matrix.length;
//
//        if(excludedNodes == null){
//            excludedNodes = new HashSet<>();
//        }
//        if(from >= len || to >= len){
//            System.out.println("异常：起始点" + from + "与终点" + to + "不能相同 或 起始点（终点）超出范围 ！");
//            return null;
//        }
//        if(!excludedNodes.isEmpty() && (excludedNodes.contains(from) || excludedNodes.contains(to))){
//            System.out.println("异常：起始点" + from + "或终点" + to + "在排除考虑的数组中！");
//            return null;
//        }
//
//        //各个节点到起点的最短路径长度
//        double[] length = new double[len];
//        Set<Integer> visited = new HashSet<>(); //已经求出最短路的节点
//        Set<Integer> notVisited = new HashSet<>(); //没有求出最短路的节点
//        //存储各个节点的前驱节点
//        int[] preNode = new int[len];
//
//        //初始化输出路径
//        for (int i = 0; i < len; i++) {
//            preNode[i] = from;
//            length[i] = MAX_VALUE;
//            if(i != from){
//                notVisited.add(i);
//            }
//        }
//
//        //初始化源节点
//        length[from] = 0;
//        visited.add(from);
//        preNode[from] = -1;
//
//        for (int i = 1; i < len-excludedNodes.size(); i++) {
//            double min = Double.MAX_VALUE;
//            int midNode = -1; //记录距离出发点最近的尚未访问过的节点索引
//
//            //从尚未访问过的节点中查找距离出发点最近的节点
//            for (int j : notVisited) {
//                if(!excludedNodes.isEmpty() && excludedNodes.contains(j)){
//                    continue;
//                }
//                //已经求出最短路径的节点不需要再加入计算， 判断加入节点后是否存在更短路径
//                if (length[j] < min) {
//                    min = length[j];
//                    midNode = j;
//                }
//            }
//
//            //更新最短路径
//            length[midNode] = min;
//            visited.add(midNode);
//
//            //更新源点将节点midNode作为中间节点到达其它节点的距离
//            for (int m : visited) {
//                if(!excludedNodes.isEmpty() && excludedNodes.contains(m)){
//                    continue;
//                }
//
//                //节点尚未访问过，且出发点经由节点index到节点m的距离比当前出发点到节点m的距离小
//                if (length[midNode] + matrix[midNode][m] < length[m]) {
//                    length[m] = length[midNode] + matrix[midNode][m];
//                    preNode[m] = midNode;
//                }
//            }
//        }
//        return new DijkstraResult<Double>(length[to], getPath(preNode, to));
//    }



    /**
     *  查找最短有效路径，有效路径指停车换乘公共交通后不能再使用小汽车前往目的地
     * @param matrix
     * @param from
     * @param to
     * @return
     */
    public static DijkstraResult<Integer> newDijkstra(int[][] matrix, int from, int to) {
        int len = matrix.length;
        Set<Integer> visited = new HashSet<>(); //已经求出最短路的节点
        Set<Integer> notVisited = new HashSet<>(); //没有求出最短路的节点
        int[] preNode = new int[len]; //前驱节点列表
        int[] length = new int[len]; //出发点到各点的最短距离
        boolean[] hasParkAndRide = new boolean[len]; //从出发点到各节点的路径上是否停车换乘

        for (int i = 0; i < len; i++) {
            preNode[i] = -1;
            length[i] = MAX_VALUE;
            if (i != from) {
                notVisited.add(i);
            }
        }

        visited.add(from);
        length[from] = 0;
        int midNode = from;

        while(!visited.contains(to)){
            for (int node : notVisited) {
                if (!hasParkAndRide[midNode] || (hasParkAndRide[midNode] && type[midNode][node] != 1)) {
                    //两种合法情况
                    //情况1：没有停车换乘
                    //情况2：已经停车换乘，但节点midNode和node之间不是汽车路段
                    if (length[midNode] + matrix[midNode][node] < length[node]) {
                        length[node] = length[midNode] + matrix[midNode][node];
                        preNode[node] = midNode;
                    }
                }
            }

            int min = MAX_VALUE;
            for (int j : notVisited) {
                if (length[j] < min) {
                    min = length[j];
                    midNode = j;
                }
            }

            visited.add(midNode);
            notVisited.remove(midNode);
            hasParkAndRide[midNode] = hasParkAndRide[preNode[midNode]] || (type[preNode[midNode]][midNode] == 2 || type[preNode[midNode]][midNode] == 3);
        }
        System.out.println(Arrays.toString(hasParkAndRide));

        return new DijkstraResult<Integer>(length[to], getPath(preNode, to));
    }


    /**
     * 在停车场停车后，寻找从停车场节点到目的节点的最短路
     * @param matrix
     * @param from
     * @param to
     * @return
     */
    public static DijkstraResult<Double> dijkstraAfterPark(double[][] matrix, int from, int to){
        int len = matrix.length;
        Set<Integer> carNodes = SimulationMain.carNodes;
        if(carNodes.contains(from)){
            carNodes.remove(from);
        }
        Set<Integer> visited = new HashSet<>(); //已经求出最短路的节点
        Set<Integer> notVisited = new HashSet<>(); //没有求出最短路的节点
        int[] preNode = new int[len]; //前驱节点列表
        double[] length = new double[len]; //出发点到各点的最短距离

        for (int i = 0; i < len; i++) {
            preNode[i] = -1;
            length[i] = MAX_VALUE;
            if (i != from) {
                notVisited.add(i);
            }
        }

        visited.add(from);
        length[from] = 0;
        int midNode = from;

        while(!visited.contains(to)){
            for (int node : notVisited) {
                if (!carNodes.contains(node)) { //如果当前节点不是汽车网络节点
                    if (length[midNode] + matrix[midNode][node] < length[node]) {
                        length[node] = length[midNode] + matrix[midNode][node];
                        preNode[node] = midNode;
                    }
                }
            }

            double min = MAX_VALUE;
            for (int j : notVisited) {
                if (length[j] < min) {
                    min = length[j];
                    midNode = j;
                }
            }

            visited.add(midNode);
            notVisited.remove(midNode);
        }

        return new DijkstraResult<Double>(length[to], getPath(preNode, to));
    }


    /**
     * 二维数组深拷贝
     * @param originalMatrix 原数组
     * @return 拷贝得到的数组
     */
    static double[][] copyMatrix(double[][] originalMatrix){
        int rows = originalMatrix.length;
        int cols = originalMatrix[0].length;

        double[][] copy = new double[rows][cols];
        for(int i=0; i<rows; i++){
            copy[i] = originalMatrix[i].clone();
        }
        return copy;
    }

}
