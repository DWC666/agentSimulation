package core;

import entity.Link;
import entity.ParkingLot;
import entity.Traveller;
import jxl.write.WriteException;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static util.ChartUtil.createXYDataset;

/*
* agent仿真主函数
* */
public class SimulationMain {
    public static final int INT_MAX = 999999;
    public static final double DOUBLE_MAX = 999999.0;
    private static final int nodeNum = 56; //节点数量
    private static final int linkNum = 204; //路段数量
    private static int carLinkNum = 52; //汽车路段数量
    public static final int parkingLotNum = 6; //停车场数量

    private static final int originNum = 2; //起点数量
    private static final int destinationNum = 2; //终点数量
    private static double bookingTravellerPercentage = 0; //预约停车用户百分比
    private static final double commuterPercentage = 50; //高峰时期通勤出行者百分比
    private static final double highIncomePercentage = 50; //高收入出行者百分比

    public static double metroDepartureInterval = 6 * 60; //地铁发车间隔（秒）
    public static double busDepartureInterval = 15 * 60; //公交发车间隔（秒）
    public static final double currency2timeFactor  = 2; //货币-时间费用折算系数
    public static final double comfort2timeFactor  = 1; //舒适度-时间费用折算系数
    public static final double carComfortCost  = 0.01 / 60; //汽车舒适度损耗系数（/秒）
    public static final double metroComfortCost = 0.1 / 60; //地铁舒适度损耗系数（/秒）
    public static final double busComfortCost = 0.2 / 60; //公交车舒适度损耗系数（/秒）
    public static final double walkComfortCost  = 0.3 / 60; //步行舒适度损耗系数（/秒）
    public static final double fuelCostPerMeter  = 0.0008; //燃油费（元/米）
    public static  double metroTicketPrice  = 4; //地铁票价（元）
    public static  double busTicketPrice  = 2; //公交票价（元）

    /*广义成本函数中 时间成本、货币成本、舒适度成本的权重系数*/
    public static final double timeWeightOfHighIncome  = 0.5; //高收入者的时间成本权重
    public static final double currencyWeightOfHighIncome  = 0.2; //高收入者的货币成本权重
    public static final double comfortWeightOfHighIncome  = 1 - timeWeightOfHighIncome - currencyWeightOfHighIncome; //高收入者的舒适度成本权重
    public static final double timeWeightOfLowIncome  = 0.2; //低收入者的时间成本权重
    public static final double currencyWeightOfLowIncome  = 0.5; //低收入者的货币成本权重
    public static final double comfortWeightOfLowIncome = 1 - timeWeightOfLowIncome - currencyWeightOfLowIncome; //低收入者的舒适度成本权重

    public static final int stepSize = 60; //迭代步长（秒）
    private static final int simulationTime = 24 * 60 * 60; //仿真总时长（秒）
    public static final int iterationNum = simulationTime / stepSize; //迭代次数
//    public static final int iterationNum = 20;
    private static final int iterationThreshold = (int)(iterationNum * 0.8); //迭代上限值，剩余迭代次数为冷却时间
    public static final double parkAndRideThreshold = 0.01; //当前节点到目的地的距离与起终点距离的比值小于该值时，不在考虑停车换乘方案

    private static AtomicInteger idGenerator = new AtomicInteger(0); //id生成器

    public static Map<Integer, ParkingLot> parkingLotMap = new HashMap<>();
    private static Map<Integer, Link> linkMap = new HashMap<>();
    private static Map<Integer, Traveller> travellerMap = new HashMap<>();
    private static Map<Integer, Traveller> finishedTravellerMap = new HashMap<>(); //已经完成旅行的出行者集合


    //停车场泊位总量
    private static final int[] parkingLotCapacity = {800, 800, 400, 400, 400, 400};
    //可预约泊位占总泊位数量的比例
    private static final double bookableBerthRateOfTotalBerth = bookingTravellerPercentage / 100;
    //停车场已使用的普通泊位占普通泊位总数的比例
    private static final double usedGeneralBerthRate = 0;
    //停车场已使用的可预约泊位占预约泊位总数的比例
    private static final double usedBookableBerthRate = 0;
    //停车场在汽车网络和步行网络的对应的节点id
    private static final int[][] parkingLotIdOfCarAndWalk = {{1, 21}, {18, 38}, {9, 27}, {10, 28},  {13, 32}, {12, 31}};
    //停车场节点相邻的汽车网络节点
    private static final Integer[][] adjacentCarNodes = {{0, 2}, {17, 19}, {3, 7, 10},  {4, 8, 9}, {8, 12, 16}, {7, 13, 15}};
    //换乘停车场id
    private static final Set<Integer> pAndRPark = new HashSet<>(Arrays.asList(new Integer[]{0, 1}));

    private static final int[] originalNodes = {20, 39}; //出发节点集合
    private static final int[] destinationNodes = {29, 30}; //目标节点集合

    public static Set<Integer> carNodes = new HashSet<>();
    public static Set<Integer> publicTrafficNodes = new HashSet<>();
    public static Set<Integer> walkNodes = new HashSet<>();
    public static Set<Integer> notCarNodes = new HashSet<>();

    //记录每次迭代中汽车路段的流量
    private static int[][] linkVolumeRecord = new int[carLinkNum][iterationNum];
    //记录每次迭代中汽车路段的旅行速度
    private static int[][] linkTravelSpeedRecord = new int[carLinkNum][iterationNum];
    //记录每次迭代中汽车路段的旅行时间
    private static int[][] linkTravelTimeRecord = new int[carLinkNum][iterationNum];

    //记录出行者出行花费的时间
    private static int[] travelerTimeRecord;
    //记录平均停车巡游时间
    private static int avgCruisingTimeRecord;
    //记录每次迭代中停车场泊位使用率
    public static double[][] totalBerthOccupy = new double[parkingLotNum][iterationNum];
    public static double[][] usedGeneralBerthOc = new double[parkingLotNum][iterationNum];
    public static double[][] usedBookableBerthOc= new double[parkingLotNum][iterationNum];
    public static double[][] queueNumber = new double[parkingLotNum][iterationNum];
    public static double[][] parkFee = new double[parkingLotNum][iterationNum];
    //记录市中心道路饱和度
    public static double[][] linkRate = new double[parkingLotNum][iterationNum];
    public static double[][] linkVolumeOf10 = new double[1][iterationNum];

    //每次迭代生成的OD数
    private static double[] ODCounterPerMinute = new double[iterationNum];
    //预约费用在总成本的占比
    private static List<Double> bookingFeeRate = new ArrayList<>();


    //总成本矩阵，即广义成本矩阵，考虑时间、货币费用、舒适性成本等因素
    public static double[][] totalCostMatrix = new double[nodeNum][nodeNum];
    public static double[][] travelTimeMatrix = new double[nodeNum][nodeNum];
    public static double[][] lengthMatrix;
    public static int[][] idMatrix;
    public static int[][] typeMatrix;

    private static Logger logger = LoggerFactory.getLogger(SimulationMain.class);


    /**
     * 初始化路网及其他参数
     */
    private static void init(){
        finishedTravellerMap.clear();
        travellerMap.clear();
        linkMap.clear();
        parkingLotMap.clear();

        //初始化停车场
        for(int i=0; i<parkingLotNum; i++){
            ParkingLot p = new ParkingLot(i);
            p.setTotalBerth(parkingLotCapacity[i]);
            p.setBookableBerth((int) (p.getTotalBerth() * bookableBerthRateOfTotalBerth));
            p.setGeneralBerth(p.getTotalBerth() - p.getBookableBerth());
            if(i != 0 && i != 1){
                p.setUsedGeneralBerth((int)(usedGeneralBerthRate * p.getGeneralBerth()));
                p.setUsedBookableBerth((int)(usedBookableBerthRate * p.getBookableBerth()));
            }
            p.setNodeIdForCarMode(parkingLotIdOfCarAndWalk[i][0]);
            p.setNodeIdForWalkMode(parkingLotIdOfCarAndWalk[i][1]);
            p.setAdjacentCarNodes(Arrays.asList(adjacentCarNodes[i]));
            if(pAndRPark.contains(i)){
                p.setParkAndRide(true);
            }
            parkingLotMap.put(i, p);
        }
        System.out.println("初始化停车场完毕！");


        //初始化网络矩阵
        ExcelUtil.MatrixResult mapMatrix = ExcelUtil.getMapMatrix();
        lengthMatrix = mapMatrix.lengthMatrix;
        idMatrix = mapMatrix.idMatrix;
        typeMatrix = mapMatrix.typeMatrix;

//        lengthMatrix = MyUtils.linkLengthMatrix;
//        travelTimeMatrix = new double[nodeNum][nodeNum];;
//        idMatrix = MyUtils.linkIdMatrix;

        for(int i=0; i<nodeNum; i++){
            Arrays.fill(travelTimeMatrix[i], DOUBLE_MAX); //将旅行时间初始化为最大值
            Arrays.fill(totalCostMatrix[i], DOUBLE_MAX); //将旅行时间初始化为最大值
        }


        //初始化路段
        for(int i=0; i<nodeNum; i++){
            for(int j=0; j<nodeNum; j++){
                if(i == j){
                    travelTimeMatrix[i][j] = 0.0;
                    continue;
                }
                int linkId = idMatrix[i][j];
                if(linkId != -1){ //如果i,j之间存在路段
                    Link link = new Link(linkId);
                    link.setLength(lengthMatrix[i][j]);
                    link.setFromNode(i);
                    link.setToNode(j);
                    //路段类型数组：0表示步行，1表示汽车，2表示公交，3表示地铁，4为汽车转步行，5为步行转汽车，
                    // 6为公交转步行，7为步行转公交，8为地铁转步行，9为步行转地铁
                    if(typeMatrix[i][j] == 1){ //如果是汽车网络
                        if(linkId == 0 || linkId == 49){
                            link.setCapacity(2000);
                        }else{
                            link.setCapacity(1400);
                        }
                        link.setAlpha(1);
                        link.setBeta(5);
                        link.setFreeSpeed(13); //速度(m/sec)
                        link.setTrafficMode(TrafficMode.CAR);
                        link.setVolume(300); //背景流量
                        carNodes.add(i);
                        carNodes.add(j);
                    }else if(typeMatrix[i][j] == 0){ //步行网络
                        link.setTrafficMode(TrafficMode.WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                        walkNodes.add(i);
                        walkNodes.add(j);
                    }else if(typeMatrix[i][j] == 2){ //公交网络
                        link.setTrafficMode(TrafficMode.BUS);
                        link.setSpeed(MyUtils.busSpeed);
                        publicTrafficNodes.add(i);
                        publicTrafficNodes.add(j);
                    }else if(typeMatrix[i][j] == 3){ //地铁网络
                        link.setTrafficMode(TrafficMode.METRO);
                        link.setSpeed(MyUtils.metroSpeed);
                        publicTrafficNodes.add(i);
                        publicTrafficNodes.add(j);
                    }else if(typeMatrix[i][j] == 4){ //汽车转步行
                        link.setTrafficMode(TrafficMode.CAR2WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 5){ //步行转汽车
                        link.setTrafficMode(TrafficMode.WALK2CAR);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 6){ //公交转步行
                        link.setTrafficMode(TrafficMode.BUS2WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 7){ //步行转公交
                        link.setTrafficMode(TrafficMode.WALK2BUS);
                        link.setSpeed(MyUtils.walkSpeed);
                        link.setTicketPrice(busTicketPrice); //公交票价
                    }else if(typeMatrix[i][j] == 8){ //地铁转步行
                        link.setTrafficMode(TrafficMode.METRO2WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 9){ //步行转地铁
                        link.setTrafficMode(TrafficMode.WALK2METRO);
                        link.setSpeed(MyUtils.walkSpeed);
                        link.setTicketPrice(metroTicketPrice); //地铁票价
                    }else{
                        throw new RuntimeException("类型矩阵异常");
                    }
                    link.calculateTravelSpeed();
                    linkMap.put(linkId, link);
                    travelTimeMatrix[i][j] = link.getTime();
                }
            }
        }
        notCarNodes.addAll(publicTrafficNodes);
        notCarNodes.addAll(walkNodes);
        System.out.println("初始化路网完毕！\n\n");
//        MyUtils.printMatrix(idMatrix);

        linkVolumeRecord = new int[iterationNum][carLinkNum];
        linkTravelSpeedRecord = new int[iterationNum][carLinkNum];
        linkTravelTimeRecord = new int[iterationNum][carLinkNum];
    }


    private static void init(double[] rate){
        finishedTravellerMap.clear();
        travellerMap.clear();
        linkMap.clear();
        parkingLotMap.clear();

        //初始化停车场
        for(int i=0; i<parkingLotNum; i++){
            ParkingLot p = new ParkingLot(i);
            p.setTotalBerth(parkingLotCapacity[i]);
            p.setBookableBerth((int) (p.getTotalBerth() * rate[i]));
            p.setGeneralBerth(p.getTotalBerth() - p.getBookableBerth());
            if(i != 0 && i != 1){
                p.setUsedGeneralBerth((int)(usedGeneralBerthRate * p.getGeneralBerth()));
                p.setUsedBookableBerth((int)(usedBookableBerthRate * p.getBookableBerth()));
            }
            p.setNodeIdForCarMode(parkingLotIdOfCarAndWalk[i][0]);
            p.setNodeIdForWalkMode(parkingLotIdOfCarAndWalk[i][1]);
            p.setAdjacentCarNodes(Arrays.asList(adjacentCarNodes[i]));
            if(pAndRPark.contains(i)){
                p.setParkAndRide(true);
            }
            parkingLotMap.put(i, p);
        }
        System.out.println("初始化停车场完毕！");


        //初始化网络矩阵
        ExcelUtil.MatrixResult mapMatrix = ExcelUtil.getMapMatrix();
        lengthMatrix = mapMatrix.lengthMatrix;
        idMatrix = mapMatrix.idMatrix;
        typeMatrix = mapMatrix.typeMatrix;

        for(int i=0; i<nodeNum; i++){
            Arrays.fill(travelTimeMatrix[i], DOUBLE_MAX); //将旅行时间初始化为最大值
            Arrays.fill(totalCostMatrix[i], DOUBLE_MAX); //将旅行时间初始化为最大值
        }


        //初始化路段
        for(int i=0; i<nodeNum; i++){
            for(int j=0; j<nodeNum; j++){
                if(i == j){
                    travelTimeMatrix[i][j] = 0.0;
                    continue;
                }
                int linkId = idMatrix[i][j];
                if(linkId != -1){ //如果i,j之间存在路段
                    Link link = new Link(linkId);
                    link.setLength(lengthMatrix[i][j]);
                    link.setFromNode(i);
                    link.setToNode(j);
                    //路段类型数组：0表示步行，1表示汽车，2表示公交，3表示地铁，4为汽车转步行，5为步行转汽车，
                    // 6为公交转步行，7为步行转公交，8为地铁转步行，9为步行转地铁
                    if(typeMatrix[i][j] == 1){ //如果是汽车网络
                        if(linkId == 0 || linkId == 49){
                            link.setCapacity(2000);
                        }else{
                            link.setCapacity(1400);
                        }
                        link.setAlpha(1);
                        link.setBeta(5);
                        link.setFreeSpeed(13); //速度(m/sec)
                        link.setTrafficMode(TrafficMode.CAR);
                        link.setVolume(300); //背景流量
                        carNodes.add(i);
                        carNodes.add(j);
                    }else if(typeMatrix[i][j] == 0){ //步行网络
                        link.setTrafficMode(TrafficMode.WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                        walkNodes.add(i);
                        walkNodes.add(j);
                    }else if(typeMatrix[i][j] == 2){ //公交网络
                        link.setTrafficMode(TrafficMode.BUS);
                        link.setSpeed(MyUtils.busSpeed);
                        publicTrafficNodes.add(i);
                        publicTrafficNodes.add(j);
                    }else if(typeMatrix[i][j] == 3){ //地铁网络
                        link.setTrafficMode(TrafficMode.METRO);
                        link.setSpeed(MyUtils.metroSpeed);
                        publicTrafficNodes.add(i);
                        publicTrafficNodes.add(j);
                    }else if(typeMatrix[i][j] == 4){ //汽车转步行
                        link.setTrafficMode(TrafficMode.CAR2WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 5){ //步行转汽车
                        link.setTrafficMode(TrafficMode.WALK2CAR);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 6){ //公交转步行
                        link.setTrafficMode(TrafficMode.BUS2WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 7){ //步行转公交
                        link.setTrafficMode(TrafficMode.WALK2BUS);
                        link.setSpeed(MyUtils.walkSpeed);
                        link.setTicketPrice(busTicketPrice); //公交票价
                    }else if(typeMatrix[i][j] == 8){ //地铁转步行
                        link.setTrafficMode(TrafficMode.METRO2WALK);
                        link.setSpeed(MyUtils.walkSpeed);
                    }else if(typeMatrix[i][j] == 9){ //步行转地铁
                        link.setTrafficMode(TrafficMode.WALK2METRO);
                        link.setSpeed(MyUtils.walkSpeed);
                        link.setTicketPrice(metroTicketPrice); //地铁票价
                    }else{
                        throw new RuntimeException("类型矩阵异常");
                    }
                    link.calculateTravelSpeed();
                    linkMap.put(linkId, link);
                    travelTimeMatrix[i][j] = link.getTime();
                }
            }
        }
        notCarNodes.addAll(publicTrafficNodes);
        notCarNodes.addAll(walkNodes);
        System.out.println("初始化路网完毕！\n\n");

        linkVolumeRecord = new int[iterationNum][carLinkNum];
        linkTravelSpeedRecord = new int[iterationNum][carLinkNum];
        linkTravelTimeRecord = new int[iterationNum][carLinkNum];
    }

    /**
     *  根据端节点查找Link
     * @param startNode
     * @param endNode
     * @return
     */
    public static Link getLink(int startNode, int endNode){
        if(startNode < 0 || endNode < 0 || startNode >= nodeNum || endNode >= nodeNum){
            throw new RuntimeException("路段的端点异常：" + "startNode: " + startNode + " endNode: " +endNode);
        }
        int linkId = idMatrix[startNode][endNode];
//        System.out.println("getLink: linkId:" + linkId);
        return linkMap.get(linkId);
    }


    /**
     * 判断当前时间是否为出行高峰期
     * @param m 一天中的第m分钟（从0点开始计时）
     * @return
     */
    private static boolean isPeakTime(int m){
        if((m >= 7*60 && m <= 9*60) || (m >= 17*60 && m <= 19*60)){
            return true;
        }
        return false;
    }



    /**
     * 计算并返回广义成本矩阵
     * @param highIncome 是否为高收入人群
     * @param bookingPark 是否为预约停车用户
     * @return
     */
    public static double[][] calculateAndGetTotalCostMatrix(boolean highIncome,  boolean bookingPark){
        for(int key : linkMap.keySet()){
            Link link = linkMap.get(key);
            if(idMatrix[link.getFromNode()][link.getToNode()] == -1){
                throw new RuntimeException("路段属性有异常：" + link);
            }
            double totalCostOfLink = getTotalCostOfLink(link, highIncome, bookingPark);
//            logger.info("路段" + link.getId() +"总成本" + totalCostOfLink);
            totalCostMatrix[link.getFromNode()][link.getToNode()] = totalCostOfLink;
        }
        return totalCostMatrix;
    }


    /**
     *  获取路段综合成本，由 时间成本、货币成本、舒适度成本 三者的加权线性组合所得。公共交通票价及停车费转移到换乘路段计算
     * @param link
     * @param highIncome
     * @return
     */
    private static double getTotalCostOfLink(Link link, boolean highIncome, boolean bookingPark){
        double travelTime = link.getTime();
        double totalCost = 0;
        double timeCost = travelTime; //旅行时间成本
        double currencyCost = 0;
        double comfortCost = 0;
        double bookingFee = 0;
        if(link.getTrafficMode() == TrafficMode.CAR){
            currencyCost = currency2timeFactor * fuelCostPerMeter * link.getLength();
            comfortCost = comfort2timeFactor * carComfortCost * travelTime;
        }else if(link.getTrafficMode() == TrafficMode.METRO){
            comfortCost = comfort2timeFactor * metroComfortCost * travelTime;
        }else if(link.getTrafficMode() == TrafficMode.BUS){
            comfortCost = comfort2timeFactor * busComfortCost * travelTime;
        }else if(link.getTrafficMode() == TrafficMode.WALK){
            comfortCost = comfort2timeFactor * walkComfortCost * travelTime;
        }else if(link.getTrafficMode() == TrafficMode.WALK2METRO){
            timeCost += metroDepartureInterval / 2; //加上地铁候车时间
            currencyCost = currency2timeFactor * link.getTicketPrice();
            comfortCost = comfort2timeFactor * walkComfortCost * travelTime;
        }else if(link.getTrafficMode() == TrafficMode.WALK2BUS){
            timeCost += busDepartureInterval / 2; //加上公交候车时间
            currencyCost = currency2timeFactor * link.getTicketPrice();
            comfortCost = comfort2timeFactor * walkComfortCost * travelTime;
        }else if(link.getTrafficMode() == TrafficMode.METRO2WALK || link.getTrafficMode() == TrafficMode.BUS2WALK){
            comfortCost = comfort2timeFactor * walkComfortCost * travelTime;
        }else if(link.getTrafficMode() == TrafficMode.CAR2WALK){
            ParkingLot parkingLot = getParkingLotByNodeId(link.getFromNode());
            if(parkingLot != null){
                currencyCost = currency2timeFactor * parkingLot.getFee(); //加上停车费
                if(bookingPark){
                    bookingFee = currency2timeFactor * parkingLot.getBookingFee();
                    currencyCost += bookingFee; //加上预约停车费
                }
            }
            comfortCost = comfort2timeFactor * walkComfortCost * travelTime;
        }else{ //walk2car
            timeCost = 0;
            currencyCost = 0;
            comfortCost = 0;
        }
        if(highIncome){
            totalCost = timeWeightOfHighIncome * timeCost + currencyWeightOfHighIncome * currencyCost + comfortWeightOfHighIncome * comfortCost;
        }else{
            totalCost = timeWeightOfLowIncome * timeCost + currencyWeightOfLowIncome * currencyCost + comfortWeightOfLowIncome * comfortCost;
        }
        if(bookingFee != 0){
            bookingFeeRate.add(currencyWeightOfHighIncome * bookingFee / totalCost);
        }

        return totalCost;
    }


    /**
     * 通过节点id获取停车场对象
     * @param nodeId
     * @return
     */
    private static ParkingLot getParkingLotByNodeId(int nodeId){
        ParkingLot parkingLot = null;
        for(int k : parkingLotMap.keySet()){
            ParkingLot p = parkingLotMap.get(k);
            if(p.getNodeIdForCarMode() == nodeId || p.getNodeIdForWalkMode() == nodeId){
                parkingLot = p;
                break;
            }
        }
        return parkingLot;
    }



    /**
     * 根据迪杰斯特拉算法的结果获取下一个节点id
     * @param path 最短路径
     * @return 最短路径上下一个节点id
     */
    public static int getNextNodeFromPath(List<Integer> path){
        if(path.size() >= 2 && path.get(0) != path.get(1)){
            return Integer.valueOf(path.get(1));
        }else{
            return -1;
        }
    }



    /**
     * 获取驾车出行中应该前往的下一个节点
     * @return
     */
    public static int getNextNodeOfCarTravel(int currentNode, int targetNode){
        if(currentNode == targetNode){
            return -1;
        }else{
            Set<Integer> excludedNodes = SimulationMain.notCarNodes;
            excludedNodes.remove(currentNode);
//            logger.info("排除的节点：" + excludedNodes);
            DijkstraAlgorithm.DijkstraResult<Double> dijkstraResult = DijkstraAlgorithm.dijkstra(travelTimeMatrix, currentNode, targetNode, excludedNodes);
            System.out.println("路径：" + dijkstraResult.path.toString());
//            logger.info("成本：" + dijkstraResult.cost);
            return getNextNodeFromPath(dijkstraResult.path);
        }
    }


    /**
     *  在停车场停车后，寻找到终点的下一个节点
     * @param fromNode
     * @param toNode
     * @return
     */
    private static int getNextNodeAfterPark(int fromNode, int toNode){
        DijkstraAlgorithm.DijkstraResult<Double> dijkstraResult = DijkstraAlgorithm.dijkstraAfterPark(travelTimeMatrix, fromNode, toNode);
        System.out.println(dijkstraResult.path);
        return getNextNodeFromPath(dijkstraResult.path);
    }


    /**
     * 返回累积前景理论的参考点
     * @param traveller
     * @param currentNode
     * @param parkingLots
     * @param parkAndRide 为true时，表示获取停车换乘模式的参考点
     * @return 两个元素的数组，第一个为行程时间参考点，第二个为货币费用参考点
     */
    public static double[] getReferencePoint0(Traveller traveller, int currentNode,  List<ParkingLot> parkingLots, boolean parkAndRide){
        double[][] costMatrix = travelTimeMatrix;
        double timeReference = DOUBLE_MAX;
        double currencyReference = DOUBLE_MAX;
        for (ParkingLot p : parkingLots){
            if(parkAndRide){
                //开车到停车场过程
                DijkstraAlgorithm.DijkstraResult<Double> drive2park = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);
                DijkstraAlgorithm.DijkstraResult<Double> lengthOfDrive2park = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);
                //换乘停车场到目的地
                DijkstraAlgorithm.DijkstraResult<Double> afterPark = DijkstraAlgorithm.dijkstraAfterPark(costMatrix, p.getNodeIdForCarMode(), traveller.getEndNode());
                double currency = (lengthOfDrive2park.cost * fuelCostPerMeter + p.getFee() + getPublicTrafficFee(afterPark.path))* currency2timeFactor;
                double time = drive2park.cost + afterPark.cost;
                if(time < timeReference){
                    timeReference = time;
                }
                if(currency < currencyReference){
                    currencyReference = currency;
                }
            }else {
                //开车到停车场过程
                DijkstraAlgorithm.DijkstraResult<Double> drive2park = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);
                DijkstraAlgorithm.DijkstraResult<Double> lengthOfDrive2park = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);

                Set<Integer> carAndPublicTrafficNodes = new HashSet<>();
                carAndPublicTrafficNodes.addAll(SimulationMain.publicTrafficNodes);
                carAndPublicTrafficNodes.addAll(SimulationMain.carNodes);
                //从停车场步行到终点
                DijkstraAlgorithm.DijkstraResult<Double> walk2end = DijkstraAlgorithm.dijkstra(costMatrix, p.getNodeIdForCarMode(), traveller.getEndNode(), carAndPublicTrafficNodes);
                double currency = (lengthOfDrive2park.cost * fuelCostPerMeter + p.getFee()) * currency2timeFactor;
                double time = drive2park.cost + walk2end.cost;
                if(time < timeReference){
                    timeReference = time;
                }
                if(currency < currencyReference){
                    currencyReference = currency;
                }
            }
        }
        return new double[]{timeReference * CPT.cpt_acceptableFactor, currencyReference * CPT.cpt_acceptableFactor};
    }


    public static double[] getReferencePoint(Traveller traveller, int currentNode){
        double[][] costMatrix = travelTimeMatrix;
        double timeReference = DOUBLE_MAX;
        double currencyReference = DOUBLE_MAX;

        //从自驾车出行和停车换乘出行两种方案中选择成本最低的路线作为参考点
        List<ParkingLot> parkingLots1 = ParkSystem.candidateParkingLot(traveller, currentNode, false);
        List<ParkingLot> parkingLots2 = ParkSystem.candidateParkingLot(traveller, currentNode, true);
        for (ParkingLot p : parkingLots1){
            //开车到停车场过程
            DijkstraAlgorithm.DijkstraResult<Double> drive2park = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);
            DijkstraAlgorithm.DijkstraResult<Double> lengthOfDrive2park = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);

            Set<Integer> carAndPublicTrafficNodes = new HashSet<>();
            carAndPublicTrafficNodes.addAll(SimulationMain.publicTrafficNodes);
            carAndPublicTrafficNodes.addAll(SimulationMain.carNodes);
            //从停车场步行到终点
            DijkstraAlgorithm.DijkstraResult<Double> walk2end = DijkstraAlgorithm.dijkstra(costMatrix, p.getNodeIdForCarMode(), traveller.getEndNode(), carAndPublicTrafficNodes);
            double currency = (lengthOfDrive2park.cost * fuelCostPerMeter + p.getFee()) * currency2timeFactor;
            double time = drive2park.cost + walk2end.cost;
            if(time < timeReference){
                timeReference = time;
            }
            if(currency < currencyReference){
                currencyReference = currency;
            }
        }

        for(ParkingLot p : parkingLots2){
            //开车到停车场过程
            DijkstraAlgorithm.DijkstraResult<Double> drive2park = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);
            DijkstraAlgorithm.DijkstraResult<Double> lengthOfDrive2park = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes);
            //换乘停车场到目的地
            DijkstraAlgorithm.DijkstraResult<Double> afterPark = DijkstraAlgorithm.dijkstraAfterPark(costMatrix, p.getNodeIdForCarMode(), traveller.getEndNode());
            double currency = (lengthOfDrive2park.cost * fuelCostPerMeter + p.getFee() + getPublicTrafficFee(afterPark.path))* currency2timeFactor;
            double time = drive2park.cost + afterPark.cost;
            if(time < timeReference){
                timeReference = time;
            }
            if(currency < currencyReference){
                currencyReference = currency;
            }
        }

        return new double[]{timeReference * CPT.cpt_acceptableFactor, currencyReference * CPT.cpt_acceptableFactor};
    }

    /**
     * 通过CPT(累积前景理论) 比较驾车出行和停车换乘方案的成本，并选择较低成本的方案
     * @param traveller
     * @param currentNode
     * @return
     */
    public static int compareAndGetNextNodeByCPT(Traveller traveller, int currentNode){
        System.out.println("使用CPT进行路径选择");
        double[][] costMatrix = travelTimeMatrix;
        double[] referencePoints = getReferencePoint(traveller, currentNode);

        //方案1, 即开车前往目的地
        List<ParkingLot> candidateParkingLot1 = ParkSystem.candidateParkingLot(traveller, currentNode, false);
        ParkingLot targetParkingLot = null;
        double driveModeCpv = -Double.MAX_VALUE;
        List<Integer> shortestPath1 = null;
        for(ParkingLot curParkingLot : candidateParkingLot1){
            //开车到停车场过程
            DijkstraAlgorithm.DijkstraResult<Double> drive2park = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), SimulationMain.notCarNodes);
            DijkstraAlgorithm.DijkstraResult<Double> lengthOfDrive2park = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), SimulationMain.notCarNodes);

            Set<Integer> carAndPublicTrafficNodes = new HashSet<>();
            carAndPublicTrafficNodes.addAll(SimulationMain.publicTrafficNodes);
            carAndPublicTrafficNodes.addAll(SimulationMain.carNodes);
            //从停车场步行到终点
            DijkstraAlgorithm.DijkstraResult<Double> walk2end = DijkstraAlgorithm.dijkstra(costMatrix, curParkingLot.getNodeIdForCarMode(), traveller.getEndNode(), carAndPublicTrafficNodes);
            List<Integer> mergePath = DijkstraAlgorithm.mergePath(drive2park.path, walk2end.path);
            double currency = (lengthOfDrive2park.cost * fuelCostPerMeter + curParkingLot.getFee()) * currency2timeFactor; //货币转换成时间
            NormalDistribution normalDistribution = CPT.hyperPathNormalDistribution(mergePath);
            double timeCpv = CPT.cpv(-1 * referencePoints[0], normalDistribution);
            double currencyCpv = CPT.cpv4FixedValue(-1 * referencePoints[1], -1 * currency);
            double cpv;
            if(traveller.isHighIncome()){
                cpv = timeCpv * CPT.cpt_highIncomeTimeWeight + currencyCpv * CPT.cpt_highIncomeCurrencyWeight;
            }else{
                cpv = timeCpv * CPT.cpt_lowIncomeTimeWeight + currencyCpv * CPT.cpt_lowIncomeCurrencyWeight;
            }
            if(cpv > driveModeCpv){
                driveModeCpv = cpv;
                targetParkingLot = curParkingLot;
                shortestPath1 = mergePath;
            }
        }
        logger.info("    自驾车方案 路径=" + shortestPath1 + ", cpv=" + driveModeCpv);


        //方案2：停车换乘
        List<ParkingLot> candidateParkingLot2 = ParkSystem.candidateParkingLot(traveller, currentNode, true);
//        double[] referencePoints2 = getReferencePoint(traveller, currentNode, candidateParkingLot2,true);
        double parkAndRideCpv = -Double.MAX_VALUE;
        ParkingLot transferParkingLot = null;
        List<Integer> shortestPath2 = null;
        //当前节点开车到目的地的最短距离
        double current2endDistance = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost;
        //判断当前节点到目的地的距离是否达到停车换乘阈值
//        logger.info("换乘距离阈值：" + current2endDistance / traveller.getStart2endDistance());
        if(current2endDistance / traveller.getStart2endDistance() >= parkAndRideThreshold){
            for(ParkingLot curParkingLot : candidateParkingLot2) {
                //当前节点开车到换乘停车场的最短距离
                double current2parkDistance = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), SimulationMain.notCarNodes).cost;
                DijkstraAlgorithm.DijkstraResult<Double> dijkstraResult = DijkstraAlgorithm.dijkstra4ParkAndRide(costMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
                //如果路径中没有真正乘坐公共交通，则不考虑该路线
                if(!DijkstraAlgorithm.takePublicTraffic(dijkstraResult.path)){
                    continue;
                }
                double publicTrafficFee = getPublicTrafficFee(dijkstraResult.path);
                double currency = (current2parkDistance * fuelCostPerMeter + curParkingLot.getFee() + publicTrafficFee) * currency2timeFactor; //货币转换成时间
                double timeCpv = CPT.cpv(-1 * referencePoints[0], CPT.hyperPathNormalDistribution(dijkstraResult.path));
                double currencyCpv = CPT.cpv4FixedValue(-1 * referencePoints[1], -1 * currency);
                double cpv;
                if(traveller.isHighIncome()){
                    cpv = timeCpv * CPT.cpt_highIncomeTimeWeight + currencyCpv * CPT.cpt_highIncomeCurrencyWeight;
                }else{
                    cpv = timeCpv * CPT.cpt_lowIncomeTimeWeight + currencyCpv * CPT.cpt_lowIncomeCurrencyWeight;
                }

                if(cpv > parkAndRideCpv){
                    parkAndRideCpv = cpv;
                    transferParkingLot = curParkingLot;
                    shortestPath2 = dijkstraResult.path;
                }
            }
        }
        logger.info("    停车换乘方案 路径=" + shortestPath2 + ", cpv=" + parkAndRideCpv);

        //如果所有停车场都没有空余泊位，则巡游至下一个停车场
        if(targetParkingLot == null && transferParkingLot == null){
            logger.info("所有停车场都没有空余泊位，巡游至下一个停车场");
            return getNextNodeOfCarTravel(currentNode, ParkSystem.selectParkingLotWhenAllIsFull(traveller, currentNode).getNodeIdForCarMode());
        }

        //选择累积前景值较大的
        if(driveModeCpv >= parkAndRideCpv){
            logger.info("    选择自驾车");
//            throw new RuntimeException("通勤者选择自驾车");
            traveller.setChooseParkAndRide(false);
            traveller.setTargetParkingLot(targetParkingLot);
            return getNextNodeFromPath(shortestPath1);
        }else{
            logger.info("    选择停车换乘");
            traveller.setChooseParkAndRide(true);
            traveller.setTargetParkingLot(transferParkingLot);
            return getNextNodeFromPath(shortestPath2);
        }
    }


    /**
     *  选择驾车和停车换乘两种方案中的最优方案并返回下一个节点
     * @param traveller
     * @param currentNode
     * @return 返回下一个节点
     */
    public static int compareAndGetNextNodeOfTravel(Traveller traveller, int currentNode) {
        logger.info("比较出行方案：" + "出行者id=" + traveller.getId() + ", 当前节点=" + currentNode + ", 目的节点=" + traveller.getEndNode());
        //如果是通勤出行者，则采用累积前景理论选择出行方案
        if(traveller.isCommuter()){
            return compareAndGetNextNodeByCPT(traveller, currentNode);
        }

        //如果是预约停车用户且尚未完成停车，则直接前往预约停车场
        if(traveller.isBookingParkUser() && !traveller.isHasPark()){
            logger.info("预约用户，直接前往预约停车场");
            return getNextNodeOfCarTravel(currentNode, traveller.getTargetParkingLot().getNodeIdForCarMode());
        }

        double[][] costMatrix = calculateAndGetTotalCostMatrix(traveller.isHighIncome(), traveller.isBookingParkUser());

        //方案1, 即开车前往目的地
        List<ParkingLot> candidateParkingLot1 = ParkSystem.candidateParkingLot(traveller, currentNode, false);
        ParkingLot targetParkingLot = null;
        double driveModeCost = Double.MAX_VALUE;
        List<Integer> shortestPath1 = null;
        for( ParkingLot curParkingLot : candidateParkingLot1){
            //开车到停车场过程
            DijkstraAlgorithm.DijkstraResult<Double> drive2park = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), SimulationMain.notCarNodes);

            Set<Integer> excludedNodes = new HashSet<>();
            excludedNodes.addAll(SimulationMain.publicTrafficNodes);
            excludedNodes.addAll(SimulationMain.carNodes);
            excludedNodes.remove(curParkingLot.getNodeIdForCarMode());
            //从停车场步行到终点
            DijkstraAlgorithm.DijkstraResult<Double> walk2end = DijkstraAlgorithm.dijkstra(costMatrix, curParkingLot.getNodeIdForCarMode(), traveller.getEndNode(), excludedNodes);
            double cost = drive2park.cost + walk2end.cost;
            if(cost < driveModeCost){
                driveModeCost = cost;
                targetParkingLot = curParkingLot;
                shortestPath1 = DijkstraAlgorithm.mergePath(drive2park.path, walk2end.path);
            }
        }
        logger.info("    自驾车方案 路径=" + shortestPath1 + ", 成本=" + driveModeCost);

        //方案2：停车换乘
        List<ParkingLot> candidateParkingLot2 = ParkSystem.candidateParkingLot(traveller, currentNode, true);
        double parkAndRideCost = Double.MAX_VALUE;
        ParkingLot targetTransferParkingLot = null;
        List<Integer> shortestPath2 = null;
        //当前节点开车到目的地的最短距离
        double current2endDistance = DijkstraAlgorithm.dijkstra(lengthMatrix, currentNode, traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost;
        //判断当前节点到目的地的距离是否达到停车换乘阈值
        if(current2endDistance / traveller.getStart2endDistance() >= parkAndRideThreshold){
            for(ParkingLot curParkingLot : candidateParkingLot2) {
                DijkstraAlgorithm.DijkstraResult<Double> dijkstraResult = DijkstraAlgorithm.dijkstra4ParkAndRide(costMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
                //如果路径中没有真正乘坐公共交通，则不考虑该路线
                if(!DijkstraAlgorithm.takePublicTraffic(dijkstraResult.path)){
                    continue;
                }
                if(dijkstraResult.cost < parkAndRideCost){
                    parkAndRideCost = dijkstraResult.cost;
                    targetTransferParkingLot = curParkingLot;
                    shortestPath2 = dijkstraResult.path;
                }
            }
        }
        logger.info("    停车换乘方案 路径=" + shortestPath2 + ", 成本=" + parkAndRideCost);

        //如果所有停车场都没有空余泊位，则巡游至下一个停车场
        if(targetParkingLot == null && targetTransferParkingLot == null){
            logger.info("所有停车场都没有空余泊位，巡游至下一个停车场");
            return getNextNodeOfCarTravel(currentNode, ParkSystem.selectParkingLotWhenAllIsFull(traveller, currentNode).getNodeIdForCarMode());
        }

        //比较两种方案的成本
        if(-driveModeCost >= -parkAndRideCost){
            logger.info("    选择自驾车");
            traveller.setTargetParkingLot(targetParkingLot);
            logger.info("目标停车场：" + targetParkingLot.getId());
            traveller.setChooseParkAndRide(false);
            return getNextNodeFromPath(shortestPath1);

        }else{
            logger.info("    选择停车换乘");
            traveller.setChooseParkAndRide(true);
            traveller.setTargetParkingLot(targetTransferParkingLot);
            logger.info("目标停车场：" + targetTransferParkingLot.getId());
            return getNextNodeFromPath(shortestPath2);
        }
    }


    /**
     * 从出行路径获取公共交通票价
     * @param path
     * @return
     */
    public static double getPublicTrafficFee(List<Integer> path){
        double fee = 0.0;
        for(int i=0; i<path.size()-1; i++) {
            Link link = SimulationMain.getLink(path.get(i), path.get(i + 1));
            fee += link.getTicketPrice();
        }
        return fee;
    }


    /**
     * 检查旅行时间矩阵是否存在异常
     * @param travelTimeMatrix
     * @param max
     * @return 第一个元素为1时表示矩阵出现问题，后两个元素为出现问题的坐标
     */
    public static int[] checkTravelTimeMatrix(double[][] travelTimeMatrix, double max){
        int[] result = new int[3]; //第一个元素为1时表示矩阵出现问题，后两个元素为出现问题的坐标
        for(int i=0; i<nodeNum; i++){
            for(int j=0; j<nodeNum; j++){
                if(i != j && idMatrix[i][j] == -1){
                    if(travelTimeMatrix[i][j] != max){
                        result[0] = 1;
                        result[1] = i;
                        result[2] = j;
                        return result;
                    }
                }
            }
        }
        return result;
    }


    /**
     * 仿真函数
     */
    public static void simulate(int count){
        int drivingCount = 0; //统计驾车前往目的地的人数

        List<Double> bookingFee = new ArrayList<>();
        List<Double> generalBerthOccupancy = new ArrayList<>();
        List<Double> bookableBerthOccupancy = new ArrayList<>();
        List<Integer> usedBookableBerth = new ArrayList<>();
        List<Integer> usedGeneralBerth = new ArrayList<>();
        List<Double> saturation = new ArrayList<>();
        List<Double> a = new ArrayList<>();

        init();

        for(int m=1; m<=iterationNum; m++){
            System.out.println("\n\n\n\n#############################    迭代次数：" + m +"   ##############################################");

            //根据泊位占有率和交通饱和度来更新预约费用
//            ParkSystem.updateParkFee(m);
            if(m % ParkSystem.bookingFeeUpdateStep == 0){
                ParkingLot p = parkingLotMap.get(2);
                bookingFee.add(ParkSystem.getFee(p.getBerthOccupancy(), ParkSystem.adjacentLinksSaturation(p)));
//                generalBerthOccupancy.add(parkingLotMap.get(2).getGeneralBerthOccupancy());
//                usedGeneralBerth.add(parkingLotMap.get(2).getUsedGeneralBerth());
                bookableBerthOccupancy.add(p.getBerthOccupancy());
                usedBookableBerth.add(p.getUsedBookableBerth());
                saturation.add(ParkSystem.adjacentLinksSaturation(p));
                a.add(ParkSystem.cruiseTime0(p.getBerthOccupancy()));
            }

            int travelerGeneratedPerIter = 0; //每次迭代生成的出行者数量

            /*---------- 步骤1：生成出行者  -----------*/
            if(m <= iterationThreshold){
                //根据动态调节函数随机决定是否生成OD出行
                double[][] dynamicOD = MyUtils.getDynamicOD2(MyUtils.baseOD, m);
                for(int i=0; i<originNum; i++){
                    for(int j=0; j<destinationNum; j++){
                        int num = (int)Math.ceil(dynamicOD[i][j]);
                        for(int k=0; k<num; k++){
                            //在i-j之间生成一次出行
                            travelerGeneratedPerIter++;
                            Traveller traveller = new Traveller(idGenerator.getAndIncrement());

                            //按概率设置相关属性
                            if(MyUtils.fixedProbabilityReturnTrue(bookingTravellerPercentage)){
                                traveller.setBookingParkUser(true);
                            }
                            if(MyUtils.fixedProbabilityReturnTrue(highIncomePercentage)){
                                traveller.setHighIncome(true);
                            }
                            if(isPeakTime(m) && MyUtils.fixedProbabilityReturnTrue(commuterPercentage)){
                                traveller.setCommuter(true);
                            }

                            traveller.setStartNode(originalNodes[i]);
                            traveller.setEndNode(destinationNodes[j]);
                            traveller.getPath().add(originalNodes[i]);
                            traveller.setStart2endDistance(DijkstraAlgorithm.dijkstra(lengthMatrix, traveller.getStartNode(), traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost);
                            traveller.setDepartureTime(m);
                            traveller.setLastPassedNode(originalNodes[i]); //刚刚路过的节点
                            if(traveller.isBookingParkUser()){ //预约停车用户
                                int nextNode = ParkSystem.bookBerthAndGetNextNode(traveller, traveller.getLastPassedNode());
                                traveller.setNextNode(nextNode);
                            }
                            if(!traveller.isBookingParkUser()){ //非预约停车用户
                                int nextNode = compareAndGetNextNodeOfTravel(traveller, traveller.getLastPassedNode());
                                traveller.setNextNode(nextNode);
                            }

                            traveller.getPath().add(traveller.getNextNode());
                            Link curLink = getLink(traveller.getLastPassedNode(), traveller.getNextNode());
                            if(curLink == null){
                                throw new RuntimeException("路段" + traveller.getLastPassedNode()+ "--" + traveller.getNextNode()+ "不存在");
                            }
                            traveller.setCurrentLink(curLink);
                            traveller.setDistanceToNextNode(curLink.getLength());
                            traveller.setTrafficMode(curLink.getTrafficMode());
                            traveller.setFirstArriveToLink(true);
                            travellerMap.put(traveller.getId(), traveller);
                            logger.info("出行者目标停车场: " + traveller.getTargetParkingLot());
                            logger.info("生成出行者: " + traveller.toString());
                        }
                    }
                }
            }
            ODCounterPerMinute[m-1] = travelerGeneratedPerIter;



//            if(m  == 1){
//                //在i-j之间生成一次出行
//                int i = 0;
//                int j = 0;
//                int num = 1;
//                for(int k=0; k<num; k++){
//                    //在i-j之间生成一次出行
//                    travelerGeneratedPerIter++;
//                    Traveller traveller = new Traveller(idGenerator.getAndIncrement());
//                    System.out.println("生成出行者: " + traveller.getId());
//
////                    traveller.setBookingParkUser(true);
//                    //按概率设置相关属性
////                    if(MyUtils.fixedProbabilityReturnTrue(bookingTravellerPercentage)){
////                        traveller.setBookingParkUser(true);
////                    }
////                    if(MyUtils.fixedProbabilityReturnTrue(highIncomePercentage)){
////                        traveller.setHighIncome(true);
////                    }
////                    if(isPeakTime(m) && MyUtils.fixedProbabilityReturnTrue(commuterPercentage)){
////                        traveller.setCommuter(true);
////                    }
////                    traveller.setCommuter(true);
//                    traveller.setBookingParkUser(true);
//                    traveller.setStartNode(originalNodes[i]);
//                    traveller.setEndNode(destinationNodes[j]);
//                    traveller.getPath().add(originalNodes[i]);
//                    traveller.setDepartureTime(m);
//                    traveller.setLastPassedNode(originalNodes[i]); //刚刚路过的节点
//
//                    if(traveller.isBookingParkUser()){ //预约停车用户
//                        int nextNode = ParkSystem.bookBerthAndGetNextNode(traveller, traveller.getLastPassedNode());
//                        traveller.setNextNode(nextNode);
//                    }
//                    if(!traveller.isBookingParkUser()){ //非预约停车用户
//                        int nextNode = compareAndGetNextNodeOfTravel(traveller, traveller.getLastPassedNode());
//                        traveller.setNextNode(nextNode);
//                    }
//
//                    traveller.getPath().add(traveller.getNextNode());
//                    Link curLink = getLink(traveller.getLastPassedNode(), traveller.getNextNode());
//                    if(curLink == null){
//                        throw new RuntimeException("路段" + traveller.getLastPassedNode()+ "--" + traveller.getNextNode()+ "不存在");
//                    }
//                    traveller.setCurrentLink(curLink);
//                    traveller.setDistanceToNextNode(curLink.getLength());
//                    traveller.setTrafficMode(curLink.getTrafficMode());
//                    traveller.setFirstArriveToLink(true);
//                    travellerMap.put(traveller.getId(), traveller);
//                    System.out.println("生成出行者: " + traveller);
//                }
//            }


            /*---------- 步骤2：移动出行者 -----------*/
            System.out.println("\n\n--------- 移动出行者 -----------");
            for(int key : travellerMap.keySet()){
                Traveller traveller = travellerMap.get(key);
                Link link = traveller.getCurrentLink();
                if(link == null){
                    throw new RuntimeException("路段" + traveller.getCurrentLink() + "不存在!");
                }

                if(traveller.getDistanceToNextNode() == 0.0){ //已经到达节点的出行者跳过该步骤
                    continue;
                }

                logger.info("移动出行者: " + traveller.toString());
                if(traveller.getTrafficMode() == TrafficMode.CAR){
                    //更新路段流量
                    if(traveller.isFirstArriveToLink()){ //如果是首次到达当前路段，则更新当前路段流量
                        link.setVolume(link.getVolume() + 1);
                        traveller.setFirstArriveToLink(false);
                        if(traveller.getLastLink() != null){ //如果上一条路段存在且为汽车路段，则上一路段流量减1
                            Link lastLink = traveller.getLastLink();
                            if(lastLink.getTrafficMode() == TrafficMode.CAR){
                                lastLink.setVolume(lastLink.getVolume() - 1);
                            }
                        }
                    }

                    //记录里程 及 更新位置（必须先记录里程，后更新位置）
                    traveller.setCarDistance(traveller.getCarDistance() + Math.min(link.getSpeed()*stepSize, traveller.getDistanceToNextNode()));
                    traveller.setDistanceToNextNode(Math.max(traveller.getDistanceToNextNode() - link.getSpeed()*stepSize, 0.0));

                    if(traveller.getTargetParkingLot().getNodeIdForCarMode() == traveller.getNextNode() && traveller.getDistanceToNextNode() == 0.0){
                        traveller.setArriveParkingLot(true);
                    }

                }else{ //已经停车或者已经换乘或刚刚出发前往汽车网络
                    //如果刚到达当前路段且上一条路段存在，则更新上一路段的流量
                    if(traveller.isFirstArriveToLink() && traveller.getLastLink() != null){
                        Link lastLink = traveller.getLastLink();
                        if(lastLink.getTrafficMode() == TrafficMode.CAR){ //如果上一条路段为汽车路段
                            lastLink.setVolume(lastLink.getVolume() - 1); //上一路段流量减1
                        }
                        traveller.setFirstArriveToLink(false);
                    }
                    //更新位置（必须先记录里程，后更新位置）
                    if(link.getTrafficMode() == TrafficMode.METRO || link.getTrafficMode() == TrafficMode.BUS){
                        traveller.setMetroAndBusDistance(traveller.getMetroAndBusDistance() + Math.min(link.getSpeed()*stepSize, traveller.getDistanceToNextNode()));
                    }else{
                        traveller.setWalkDistance(traveller.getWalkDistance() + Math.min(link.getSpeed()*stepSize, traveller.getDistanceToNextNode()));
                    }
                    traveller.setDistanceToNextNode(Math.max(traveller.getDistanceToNextNode() - link.getSpeed()*stepSize, 0.0));
                }

                logger.info("移动后: " + traveller);
            }



            /*---------- 步骤3：更新旅行时间矩阵 -----------*/
            System.out.println("\n\n--------更新旅行时间矩阵-----------");
            for(int key : linkMap.keySet()){
                Link link = linkMap.get(key);
                if(idMatrix[link.getFromNode()][link.getToNode()] == -1){
                    throw new RuntimeException("路段属性有异常：" + link);
                }
                travelTimeMatrix[link.getFromNode()][link.getToNode()] = link.getTime();
            }
            //检查旅行时间矩阵是否存在异常
            int[] result = checkTravelTimeMatrix(travelTimeMatrix, DOUBLE_MAX);
            if(result[0] == 1){ //1表示出现异常
                MyUtils.printMatrix(travelTimeMatrix);
                throw new RuntimeException("旅行时间矩阵出现问题，坐标为：" + result[1] + "," + result[2]);
            }


            /*---------- 步骤4：确定出行者下一步的位置 -----------*/
            System.out.println("\n\n--------判断出行者当前位置并确定下一步的位置----------");
            for(Iterator<Map.Entry<Integer, Traveller>> iterator = travellerMap.entrySet().iterator(); iterator.hasNext();){
                Map.Entry<Integer, Traveller> entry = iterator.next();
                Traveller traveller = entry.getValue();
                Link currentLink = traveller.getCurrentLink();

                System.out.print("  判断出行者" + traveller.getId() + "的位置：");

                //情况1（该情况不需要处理，直接跳过） ：未到达节点 --> 继续向前行驶
                if(traveller.getDistanceToNextNode() != 0.0){
                    System.out.println("尚在途中，未到达节点 --> 继续向前行驶: " + traveller.getPath());
                    continue;
                }

                //情况2 ：到达目标停车场（换乘停车场 或 自驾出行停车场）
                if(traveller.isQueuing()){
                    continue;
                }
                if(traveller.getTrafficMode() == TrafficMode.CAR && traveller.isArriveParkingLot()){
                    ParkingLot targetParkingLot = traveller.getTargetParkingLot();
                    if(traveller.getFirstArriveParkingLotTime() == 0){ //设置首次到达停车场时间，以便统计分析
                        traveller.setFirstArriveParkingLotTime(m);
                    }

                    if(traveller.isChooseParkAndRide()){ //用户选择停车换乘，并到达换乘停车场
                        if(traveller.isBookingParkUser()){ //预约停车用户，直接停车
                            //可预约泊位数量在预约的时候已经扣减，现在不用扣减
                            traveller.setStartParkTime(m);
                            targetParkingLot.getBookingUserParkList().add(traveller);
                            traveller.setLastPassedNode(targetParkingLot.getNodeIdForCarMode());
                            int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
                            traveller.setNextNode(nextNode);
                            traveller.getPath().add(traveller.getNextNode());
                            Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
                            traveller.setCurrentLink(nextLink);
                            traveller.setLastLink(currentLink);
                            traveller.setDistanceToNextNode(nextLink.getLength());
                            traveller.setTrafficMode(nextLink.getTrafficMode());
//                            traveller.setFirstArriveToLink(true);
                            traveller.setHasPark(true);
                            traveller.setHasParkAndRide(true);
                            traveller.setArriveParkingLot(false);
                            System.out.println("到达停车场并完成停车:" + traveller);
                        }else{ //非预约用户
                            if(targetParkingLot.hasAvailableGeneralBerth()){ //有空余泊位
                                traveller.setStartParkTime(m);
                                targetParkingLot.addGeneralPark(traveller);
                                traveller.setLastPassedNode(targetParkingLot.getNodeIdForCarMode());
                                int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
                                traveller.setNextNode(nextNode);
                                traveller.getPath().add(traveller.getNextNode());
                                Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
                                traveller.setCurrentLink(nextLink);
                                traveller.setLastLink(currentLink);
                                traveller.setDistanceToNextNode(nextLink.getLength());
//                                traveller.setFirstArriveToLink(true);
                                traveller.setHasPark(true);
                                traveller.setHasParkAndRide(true);
                                traveller.setArriveParkingLot(false);
                                traveller.setTrafficMode(nextLink.getTrafficMode());
                                System.out.println("完成换乘停车，前往下一个节点: " + traveller);
                            }else{ //换乘停车场没有空余泊位，则比较两种出行方案，并确定下一个节点
                                System.out.println("换乘停车场没有空余泊位");
                                Queue<Integer> visitedParkingLots = traveller.getVisitedParkingLots();
                                if(!visitedParkingLots.isEmpty() && (visitedParkingLots.peek() == targetParkingLot.getId())){
                                    visitedParkingLots.poll(); //从头部删除，并添加到队列尾部，表示刚刚访问过该停车场
                                }
                                visitedParkingLots.offer(targetParkingLot.getId());
                                int currentNode = targetParkingLot.getNodeIdForCarMode();
                                int nextNode = compareAndGetNextNodeOfTravel(traveller, currentNode);
                                if(traveller.getTargetParkingLot().getId() == targetParkingLot.getId()){ //选择当前停车场排队
                                    continue;
                                }else{
                                    traveller.setLastPassedNode(currentNode);
                                    traveller.setNextNode(nextNode);
                                    traveller.getPath().add(traveller.getNextNode());
                                    Link nextLink = getLink(traveller.getLastPassedNode(), traveller.getNextNode());
                                    traveller.setCurrentLink(nextLink);
                                    traveller.setLastLink(currentLink);
                                    traveller.setDistanceToNextNode(nextLink.getLength());
                                    traveller.setTrafficMode(nextLink.getTrafficMode());
                                    traveller.setFirstArriveToLink(true);
                                    traveller.setParkAndRideModeFail(true);
                                    traveller.setChooseParkAndRide(false);
                                    traveller.setHasPark(false);
                                    traveller.setArriveParkingLot(false);
                                    System.out.println("前往下一个节点: " + nextNode);
                                }
                            }
                        }

                    }else{ //自驾车出行到达目的地附近停车场
                        if(traveller.isBookingParkUser()){ //预约停车用户，到达停车场直接停车
                            //可预约泊位数量在预约的时候已经扣减，现在不用扣减
                            traveller.setStartParkTime(m);
                            targetParkingLot.getBookingUserParkList().add(traveller);
                            traveller.setLastPassedNode(targetParkingLot.getNodeIdForCarMode());
                            int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
                            traveller.setNextNode(nextNode);
                            traveller.getPath().add(traveller.getNextNode());
                            Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
                            traveller.setCurrentLink(nextLink);
                            traveller.setLastLink(currentLink);
                            traveller.setDistanceToNextNode(nextLink.getLength());
                            traveller.setTrafficMode(nextLink.getTrafficMode());
//                            traveller.setFirstArriveToLink(true);
                            traveller.setHasPark(true);
                            traveller.setArriveParkingLot(false);
                            System.out.println("到达停车场并完成停车:" + traveller);
                        }else{ //普通停车用户
                            if(targetParkingLot.hasAvailableGeneralBerth()){ //有空余泊位
                                traveller.setStartParkTime(m);
//                                if(traveller.getStartParkTime() > traveller.getFirstArriveParkingLotTime()){
//                                    throw new RuntimeException("停车巡游时间：" + (traveller.getStartParkTime()-traveller.getFirstArriveParkingLotTime()));
//                                }
                                targetParkingLot.addGeneralPark(traveller);
                                traveller.setLastPassedNode(targetParkingLot.getNodeIdForWalkMode());
                                int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForWalkMode(), traveller.getEndNode());
                                traveller.setNextNode(nextNode);
                                Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
                                traveller.setCurrentLink(nextLink);
                                traveller.setLastLink(currentLink);
                                traveller.setDistanceToNextNode(nextLink.getLength());
                                traveller.setTrafficMode(nextLink.getTrafficMode());
//                                traveller.setFirstArriveToLink(true);
                                traveller.setHasPark(true);
                                traveller.setArriveParkingLot(false);
                                System.out.println("到达停车场并完成停车:" + traveller);
                            }else{ //没有空余泊位
                                System.out.println("停车场" + targetParkingLot.getId() + "没有泊位");
                                if(traveller.isChooseQueueUp()){ //选择排队
                                    traveller.setStartQueueTime(m);
                                    traveller.setLastLink(traveller.getCurrentLink());
                                    traveller.setQueuing(true);
                                    traveller.setChooseQueueUp(false);
                                    targetParkingLot.getWaitingQueue().offer(traveller);
                                }else{
                                    Queue<Integer> visitedParkingLots = traveller.getVisitedParkingLots();
                                    if(!visitedParkingLots.isEmpty() && (visitedParkingLots.peek() == targetParkingLot.getId())){
                                        visitedParkingLots.poll(); //从头部删除，并添加到队列尾部，表示刚刚访问过该停车场
                                    }
                                    visitedParkingLots.offer(targetParkingLot.getId());
                                    int currentNode = targetParkingLot.getNodeIdForCarMode();
                                    ParkingLot nextParkingLot = ParkSystem.selectNextParkingLot(traveller, currentNode);
                                    if(nextParkingLot.getId() == targetParkingLot.getId()){ //选择当前停车场排队
                                        continue;
                                    }else{
                                        traveller.setLastPassedNode(currentNode);
                                        traveller.setTargetParkingLot(nextParkingLot);
                                        traveller.setNextNode(getNextNodeOfCarTravel(traveller.getLastPassedNode(),nextParkingLot.getNodeIdForCarMode()));
                                        traveller.getPath().add(traveller.getNextNode());
                                        Link nextLink = getLink(traveller.getLastPassedNode(), traveller.getNextNode());
                                        traveller.setCurrentLink(nextLink);
                                        traveller.setLastLink(currentLink);
                                        traveller.setDistanceToNextNode(nextLink.getLength());
                                        traveller.setArriveParkingLot(false);
                                        traveller.setFirstArriveToLink(true);
                                        logger.info("前往下一个停车场：" + nextParkingLot);
                                    }
                                }

                            }
                        }
                    }
                }
                //情况3 ：到达普通节点 --> 到达目的地 或 决定下一个节点
                else{
                    if(traveller.isHasPark()){ //已经完成停车
                        if(traveller.getNextNode() == traveller.getEndNode()){ // 到达目的地 --> 出行结束
                            traveller.setArrivalTime(m);
                            traveller.setTravelFinished(true);
//                            if(traveller.isCommuter() && traveller.isHasParkAndRide()){
//                                throw new RuntimeException("通勤出行者到达目的地：" + traveller);
//                            }
                            finishedTravellerMap.put(traveller.getId(), traveller);
                            iterator.remove();
                            System.out.println("到达目的地: " + traveller);
                        }else{ //停车后未到达目的地 --> 前往下一个节点
                            logger.info("停车后未到达目的地");
                            int currentNode = traveller.getNextNode();
                            traveller.setLastPassedNode(currentNode);
                            int nextNodeOfPath = getNextNodeAfterPark(currentNode, traveller.getEndNode());
                            traveller.setNextNode(nextNodeOfPath);
                            Link nextLink = getLink(currentNode, nextNodeOfPath);
                            if(nextLink == null){
                                throw new RuntimeException("路段" + "currentNode:" + currentNode + "———nextNodeOfPath: " + nextNodeOfPath + "不存在");
                            }
                            traveller.setTrafficMode(nextLink.getTrafficMode());
                            traveller.getPath().add(nextNodeOfPath);
                            traveller.setLastLink(currentLink);
                            traveller.setCurrentLink(nextLink);
                            traveller.setFirstArriveToLink(true);
                            traveller.setDistanceToNextNode(nextLink.getLength());
                            System.out.println("前往下一个节点: " + nextNodeOfPath);
                        }
                    }else{ //未完成停车，正前往停车场途中
                        System.out.println("未完成停车，正前往停车场途中" + traveller.toString());
                        int currentNode = traveller.getNextNode();
                        int nextNode = compareAndGetNextNodeOfTravel(traveller, currentNode);
                        if(nextNode == -1 && traveller.getTargetParkingLot().getNodeIdForCarMode() == currentNode && traveller.isChooseQueueUp()){
                            //说明选择当前途经停车场进行排队停车
                            continue;
                        }
                        traveller.setLastPassedNode(currentNode);
                        traveller.setNextNode(nextNode);
                        if(currentNode == 18 && nextNode == -1){
                            System.out.println("路段49的旅行速度：" + linkMap.get(49).getSpeed());
                            System.out.println("路段49的流量：" + linkMap.get(49).getVolume());
                        }
                        Link nextLink = getLink(currentNode, nextNode);
                        traveller.setTrafficMode(nextLink.getTrafficMode());
                        traveller.getPath().add(nextNode);
                        traveller.setLastLink(currentLink);
                        traveller.setCurrentLink(nextLink);
                        traveller.setDistanceToNextNode(nextLink.getLength());
                        traveller.setFirstArriveToLink(true);
                        System.out.println("前往下一个节点: " + nextNode);
                    }
                }

                if(traveller.getPath().size() > linkNum/5){
                    System.out.println(traveller);
                    iterator.remove();
//                    throw new RuntimeException("出行者" + traveller.getId() + "的路径过长，存在异常：" + traveller);
                }
            }


            /*---------- 步骤5：更新停车场停车列表 -----------*/
            System.out.println("\n\n--------更新停车场停车列表----------");
            for(int key : parkingLotMap.keySet()){
                ParkingLot parkingLot = parkingLotMap.get(key);

                //检查普通停车队列
                List<Traveller> generalParkList = parkingLot.getGeneralParkList();
                if(generalParkList.size() > 0){
//                    logger.info("停车场" + parkingLot.getId() + "普通停车队列数量" + generalParkList.size());
//                    logger.info(    "已使用的普通泊位数量：" + parkingLot.getUsedGeneralBerth());
                    for(Iterator<Traveller> iterator = generalParkList.iterator(); iterator.hasNext();){
                        Traveller traveller = iterator.next();
                        if((m - traveller.getStartParkTime()) * stepSize  >= traveller.getParkTime() * 60){ //如果已经达到停车时长，则删除该车辆
                            logger.info("停车场" + parkingLot.getId() + "已使用的普通泊位数量：" + parkingLot.getUsedGeneralBerth());
                            parkingLot.decreaseUsedGeneralBerthByOne();
                            iterator.remove();
                        }
                    }
                }

                //检查预约停车队列
                List<Traveller> bookingParkList = parkingLot.getBookingUserParkList();
                if(bookingParkList.size() > 0){
                    for(int i=bookingParkList.size()-1; i>=0; i--){
                        Traveller traveller = bookingParkList.get(i);
                        if((m - traveller.getStartParkTime())*stepSize >= traveller.getParkTime() * 60){ //如果已经达到停车时长，则删除该车辆
                            parkingLot.decreaseUsedBookableBerthByOne();
                            bookingParkList.remove(traveller);
                        }
                    }
                }

                //检查排队队列
                if(!parkingLot.getWaitingQueue().isEmpty()){
                    Queue<Traveller> waitingQueue =  parkingLot.getWaitingQueue();
                    if(parkingLot.hasAvailableGeneralBerth()){
                        int berthNum = parkingLot.getGeneralBerth() - parkingLot.getUsedGeneralBerth();
                        for(int i = 0; i<Math.min(berthNum, waitingQueue.size()); i++){
                            Traveller traveller = waitingQueue.poll();
                            parkingLot.addGeneralPark(traveller);
                            traveller.setStartParkTime(m);
                            traveller.setHasPark(true);
                            traveller.setQueuing(false);
                            traveller.setArriveParkingLot(false);
                        }
                    }
                }

            }


            /*---------- 步骤6：数据统计 -----------*/
            //统计路段相关数据
            System.out.println("--------数据统计----------");
            for(int key : linkMap.keySet()){
                Link link = linkMap.get(key);
                int linkId = link.getId();
                if(link.getTrafficMode() == TrafficMode.CAR){
//                    if(linkId < carLinkNum){
////                        linkVolumeRecord[linkId][m-1] = (int)link.getVolume(); //统计各路段流量
////                        linkTravelSpeedRecord[linkId][m-1] = (int)link.getSpeed();
////                        linkTravelTimeRecord[linkId][m-1] = (int)link.getTime();
//                        linkAvgSaturation[linkId][count] = link.getSaturation();
//                    }else{
//                        linkAvgSaturation[linkId - 126 + carLinkNum - 2][count] += link.getSaturation();
//                    }

                }
            }

            //统计停车场相关数据
            for(int key : parkingLotMap.keySet()){
                ParkingLot parkingLot = parkingLotMap.get(key);
                totalBerthOccupy[parkingLot.getId()][m-1] = parkingLot.getBerthOccupancy();
                usedGeneralBerthOc[parkingLot.getId()][m-1] = parkingLot.getGeneralBerthOccupancy();
                usedBookableBerthOc[parkingLot.getId()][m-1] = parkingLot.getBookableBerthOccupancy();
                queueNumber[parkingLot.getId()][m-1] = parkingLot.getWaitingQueue().size();
                parkFee[parkingLot.getId()][m-1] = parkingLot.getFee();
                linkRate[parkingLot.getId()][m-1] = ParkSystem.adjacentLinksSaturation(parkingLot);
            }
            linkVolumeOf10[0][m-1] = linkMap.get(32).getVolume();
        }
        /*-——————————————————————————————————————————————迭代结束——————————————————————————————————————————————————————————————————*/

        //统计出行者相关数据
        int parkAndRideFailNum = 0;
        int commuterCount = 0;
        int bookParkUser = 0;
        int commuterPRCount = 0; //通勤中停车换乘人数
        double avgCarDistance = 0; //平均车公里数
        double avgParkSearchTime = 0;
        int parkSearchCount = 0;
        double commuterCarDis = 0;
        double commuterPublicDis = 0;

        int highCount = 0;
        int highPRCount = 0;
        int lowCount = 0;
        int lowPRCount = 0;
        double highCarDis = 0;
        double highPubDis = 0;
        double lowCarDis = 0;
        double lowPubDis = 0;

        totalCarDistance = 0;
        totalMetroAndBusDistance = 0;
        travelerTimeRecord = new int[idGenerator.get()];
        for(int key : finishedTravellerMap.keySet()){
            Traveller traveller = finishedTravellerMap.get(key);
            travelerTimeRecord[traveller.getId()] = traveller.getArrivalTime() - traveller.getDepartureTime(); //统计旅行者旅行时间
            if(!traveller.isHasParkAndRide()){
                avgCruisingTimeRecord += (traveller.getStartParkTime() - traveller.getFirstArriveParkingLotTime());
                drivingCount++;
            }
            if(traveller.isParkAndRideModeFail()){
                parkAndRideFailNum++;
            }
            if(traveller.isBookingParkUser()){
                bookParkUser++;
            }
            if(traveller.getStartParkTime() - traveller.getFirstArriveParkingLotTime() > 0){
                avgParkSearchTime += (traveller.getStartParkTime() - traveller.getFirstArriveParkingLotTime());
                parkSearchCount++;
            }
            if(traveller.isCommuter()){
                commuterCount++;
                if(traveller.isHasParkAndRide()){
                    commuterPRCount++;
                }
                commuterCarDis += traveller.getCarDistance();
                commuterPublicDis +=  traveller.getMetroAndBusDistance();
            }
            if(traveller.isHighIncome()){
                highCount++;
                if(traveller.isHasParkAndRide()){
                    highPRCount++;
                }
                highCarDis += traveller.getCarDistance();
                highPubDis += traveller.getMetroAndBusDistance();
            }else{
                lowCount++;
                if(traveller.isHasParkAndRide()){
                    lowPRCount++;
                }
                lowCarDis += traveller.getCarDistance();
                lowPubDis += traveller.getMetroAndBusDistance();
            }
            avgCarDistance += traveller.getCarDistance();
            totalCarDistance += traveller.getCarDistance();
            totalMetroAndBusDistance += traveller.getMetroAndBusDistance();
        }
        if(drivingCount != 0){
            avgCruisingTimeRecord /= drivingCount;
        }
        avgCarDistance /= finishedTravellerMap.size();

        int queuingNum = 0;
        for(int key : parkingLotMap.keySet()){
            ParkingLot parkingLot = parkingLotMap.get(key);
            queuingNum += parkingLot.getWaitingQueue().size();
        }

        data[0][count] =  avgCarDistance;
        data[1][count] = avgParkSearchTime / parkSearchCount;
        data[2][count] = parkSearchCount * 1.0 / drivingCount;
        data[3][count] = (finishedTravellerMap.size() - drivingCount) * 1.0 / finishedTravellerMap.size();
        data[4][count] = totalMetroAndBusDistance / (totalMetroAndBusDistance + totalCarDistance);
        data[5][count] = commuterPRCount *1.0 / commuterCount;
        data[6][count] = commuterPublicDis / (commuterPublicDis+commuterCarDis);
        System.out.println("\n\n*********** 仿真结束 ************************");
        System.out.println("生成出行者人数： " + idGenerator.get());
        System.out.println("完成出行人数：" + finishedTravellerMap.size());
        System.out.println("驾车出行人数：" + drivingCount + " 占比 " + (drivingCount * 1.0 / finishedTravellerMap.size()));
        System.out.println("停车换乘出行人数：" + (finishedTravellerMap.size()-drivingCount) + " 占比 " + ((finishedTravellerMap.size()-drivingCount) * 1.0 / finishedTravellerMap.size()));
        System.out.println("停车换乘失败人数：" + parkAndRideFailNum);

        System.out.println("预约停车人数：" + bookParkUser);
        System.out.println("正在排队停车人数：" + queuingNum);
        System.out.println("巡游人数：" + parkSearchCount);
        System.out.println("平均车里程：" + avgCarDistance);
        System.out.println("公共交通里程：" + totalMetroAndBusDistance);
        System.out.println("汽车里程：" + totalCarDistance);
        System.out.println("公共交通分担率：" + totalMetroAndBusDistance / (totalCarDistance + totalMetroAndBusDistance));
        System.out.println("通勤人数：" + commuterCount);
        System.out.println("通勤停车换乘人数：" + commuterPRCount);
        System.out.println("通勤出行者汽车出行里程：" + commuterCarDis);
        System.out.println("通勤出行者公共交通出行里程：" + commuterPublicDis);
        System.out.println("通勤出行者公共交通分担率：" + commuterPublicDis / (commuterPublicDis+commuterCarDis));

//        System.out.println("已使用的普通泊位变化：" + usedGeneralBerth.toString());
//        System.out.println("普通泊位占有率变化：" + generalBerthOccupancy.toString());
//        System.out.println("已使用的可预约泊位变化：" + usedBookableBerth.toString());
//        System.out.println("可预约泊位占有率变化：" + bookableBerthOccupancy.toString());
//        System.out.println("道路饱和度：" + saturation.toString());
//        System.out.println("巡游时间倍数：" + a.toString());
//        System.out.println("预约费用变化：" + bookingFee.toString());
//        System.out.println("预约出行驾驶模式平均成本：" + ParkSystem.arrayAverage(ParkSystem.carModeCost));
//        System.out.println("预约出行换乘模式平均成本：" + ParkSystem.arrayAverage(ParkSystem.prModeCost));
//        System.out.println("预约出行驾驶模式平均cpv：" + ParkSystem.arrayAverage(ParkSystem.carModeCPV));
//        System.out.println("预约出行换乘模式平均cpv：" + ParkSystem.arrayAverage(ParkSystem.prModeCPV));
        for(int i=0; i<data.length; i++){
            System.out.println(Arrays.toString(data[i]));
        }
    }


//    public static double simulate(double[] rate){
//        int drivingCount = 0; //统计驾车前往目的地的人数
//        List<Double> bookableBerthRate = new ArrayList<>();
//        init(rate);
//
//        for(int m=1; m<=iterationNum; m++){
//            System.out.println("\n\n\n\n#############################    迭代次数：" + m +"   ##############################################");
//
//            int travelerGeneratedPerIter = 0; //每次迭代生成的出行者数量
//
//            /*---------- 步骤1：生成出行者  -----------*/
//            if(m <= iterationThreshold){
//                //根据动态调节函数随机决定是否生成OD出行
//                double[][] dynamicOD = MyUtils.getDynamicOD2(MyUtils.baseOD, m);
//                for(int i=0; i<originNum; i++){
//                    for(int j=0; j<destinationNum; j++){
//                        int num = (int)Math.ceil(dynamicOD[i][j]);
//                        for(int k=0; k<num; k++){
//                            //在i-j之间生成一次出行
//                            travelerGeneratedPerIter++;
//                            Traveller traveller = new Traveller(idGenerator.getAndIncrement());
//
//                            //按概率设置相关属性
//                            if(MyUtils.fixedProbabilityReturnTrue(bookingTravellerPercentage)){
//                                traveller.setBookingParkUser(true);
//                            }
//                            if(MyUtils.fixedProbabilityReturnTrue(highIncomePercentage)){
//                                traveller.setHighIncome(true);
//                            }
//                            if(isPeakTime(m) && MyUtils.fixedProbabilityReturnTrue(commuterPercentage)){
//                                traveller.setCommuter(true);
//                            }
//
//                            traveller.setStartNode(originalNodes[i]);
//                            traveller.setEndNode(destinationNodes[j]);
//                            traveller.getPath().add(originalNodes[i]);
//                            traveller.setStart2endDistance(DijkstraAlgorithm.dijkstra(lengthMatrix, traveller.getStartNode(), traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost);
//                            traveller.setDepartureTime(m);
//                            traveller.setLastPassedNode(originalNodes[i]); //刚刚路过的节点
//                            if(traveller.isBookingParkUser()){ //预约停车用户
//                                int nextNode = ParkSystem.bookBerthAndGetNextNode(traveller, traveller.getLastPassedNode());
//                                traveller.setNextNode(nextNode);
//                            }
//                            if(!traveller.isBookingParkUser()){ //非预约停车用户
//                                int nextNode = compareAndGetNextNodeOfTravel(traveller, traveller.getLastPassedNode());
//                                traveller.setNextNode(nextNode);
//                            }
//
//                            traveller.getPath().add(traveller.getNextNode());
//                            Link curLink = getLink(traveller.getLastPassedNode(), traveller.getNextNode());
//                            if(curLink == null){
//                                throw new RuntimeException("路段" + traveller.getLastPassedNode()+ "--" + traveller.getNextNode()+ "不存在");
//                            }
//                            traveller.setCurrentLink(curLink);
//                            traveller.setDistanceToNextNode(curLink.getLength());
//                            traveller.setTrafficMode(curLink.getTrafficMode());
//                            traveller.setFirstArriveToLink(true);
//                            travellerMap.put(traveller.getId(), traveller);
//                            logger.info("出行者目标停车场: " + traveller.getTargetParkingLot());
//                            logger.info("生成出行者: " + traveller.toString());
//                        }
//                    }
//                }
//            }
//            ODCounterPerMinute[m-1] = travelerGeneratedPerIter;
//
//
//            /*---------- 步骤2：移动出行者 -----------*/
//            System.out.println("\n\n--------- 移动出行者 -----------");
//            for(int key : travellerMap.keySet()){
//                Traveller traveller = travellerMap.get(key);
//                Link link = traveller.getCurrentLink();
//                if(link == null){
//                    throw new RuntimeException("路段" + traveller.getCurrentLink() + "不存在!");
//                }
//
//                if(traveller.getDistanceToNextNode() == 0.0){ //已经到达节点的出行者跳过该步骤
//                    continue;
//                }
//
//                logger.info("移动出行者: " + traveller.toString());
//                if(traveller.getTrafficMode() == TrafficMode.CAR){
//                    //更新路段流量
//                    if(traveller.isFirstArriveToLink()){ //如果是首次到达当前路段，则更新当前路段流量
//                        link.setVolume(link.getVolume() + 1);
//                        traveller.setFirstArriveToLink(false);
//                        if(traveller.getLastLink() != null){ //如果上一条路段存在且为汽车路段，则上一路段流量减1
//                            Link lastLink = traveller.getLastLink();
//                            if(lastLink.getTrafficMode() == TrafficMode.CAR){
//                                lastLink.setVolume(lastLink.getVolume() - 1);
//                            }
//                        }
//                    }
//
//                    //记录里程 及 更新位置（必须先记录里程，后更新位置）
//                    traveller.setCarDistance(traveller.getCarDistance() + Math.min(link.getSpeed()*stepSize, traveller.getDistanceToNextNode()));
//                    traveller.setDistanceToNextNode(Math.max(traveller.getDistanceToNextNode() - link.getSpeed()*stepSize, 0.0));
//
//                    if(traveller.getTargetParkingLot().getNodeIdForCarMode() == traveller.getNextNode() && traveller.getDistanceToNextNode() == 0.0){
//                        traveller.setArriveParkingLot(true);
//                    }
//
//                }else{ //已经停车或者已经换乘或刚刚出发前往汽车网络
//                    //如果刚到达当前路段且上一条路段存在，则更新上一路段的流量
//                    if(traveller.isFirstArriveToLink() && traveller.getLastLink() != null){
//                        Link lastLink = traveller.getLastLink();
//                        if(lastLink.getTrafficMode() == TrafficMode.CAR){ //如果上一条路段为汽车路段
//                            lastLink.setVolume(lastLink.getVolume() - 1); //上一路段流量减1
//                        }
//                        traveller.setFirstArriveToLink(false);
//                    }
//                    //更新位置（必须先记录里程，后更新位置）
//                    if(link.getTrafficMode() == TrafficMode.METRO || link.getTrafficMode() == TrafficMode.BUS){
//                        traveller.setMetroAndBusDistance(traveller.getMetroAndBusDistance() + Math.min(link.getSpeed()*stepSize, traveller.getDistanceToNextNode()));
//                    }else{
//                        traveller.setWalkDistance(traveller.getWalkDistance() + Math.min(link.getSpeed()*stepSize, traveller.getDistanceToNextNode()));
//                    }
//                    traveller.setDistanceToNextNode(Math.max(traveller.getDistanceToNextNode() - link.getSpeed()*stepSize, 0.0));
//                }
//
//                logger.info("移动后: " + traveller);
//            }
//
//
//
//            /*---------- 步骤3：更新旅行时间矩阵 -----------*/
//            System.out.println("\n\n--------更新旅行时间矩阵-----------");
//            for(int key : linkMap.keySet()){
//                Link link = linkMap.get(key);
//                if(idMatrix[link.getFromNode()][link.getToNode()] == -1){
//                    throw new RuntimeException("路段属性有异常：" + link);
//                }
//                travelTimeMatrix[link.getFromNode()][link.getToNode()] = link.getTime();
//            }
//            //检查旅行时间矩阵是否存在异常
//            int[] result = checkTravelTimeMatrix(travelTimeMatrix, DOUBLE_MAX);
//            if(result[0] == 1){ //1表示出现异常
//                MyUtils.printMatrix(travelTimeMatrix);
//                throw new RuntimeException("旅行时间矩阵出现问题，坐标为：" + result[1] + "," + result[2]);
//            }
//
//
//            /*---------- 步骤4：确定出行者下一步的位置 -----------*/
//            System.out.println("\n\n--------判断出行者当前位置并确定下一步的位置----------");
//            for(Iterator<Map.Entry<Integer, Traveller>> iterator = travellerMap.entrySet().iterator(); iterator.hasNext();){
//                Map.Entry<Integer, Traveller> entry = iterator.next();
//                Traveller traveller = entry.getValue();
//                Link currentLink = traveller.getCurrentLink();
//
//                System.out.print("  判断出行者" + traveller.getId() + "的位置：");
//
//                //情况1（该情况不需要处理，直接跳过） ：未到达节点 --> 继续向前行驶
//                if(traveller.getDistanceToNextNode() != 0.0){
//                    System.out.println("尚在途中，未到达节点 --> 继续向前行驶: " + traveller.getPath());
//                    continue;
//                }
//
//                //情况2 ：到达目标停车场（换乘停车场 或 自驾出行停车场）
//                if(traveller.isQueuing()){
//                    continue;
//                }
//                if(traveller.getTrafficMode() == TrafficMode.CAR && traveller.isArriveParkingLot()){
//                    ParkingLot targetParkingLot = traveller.getTargetParkingLot();
//                    if(traveller.getFirstArriveParkingLotTime() == 0){ //设置首次到达停车场时间，以便统计分析
//                        traveller.setFirstArriveParkingLotTime(m);
//                    }
//
//                    if(traveller.isChooseParkAndRide()){ //用户选择停车换乘，并到达换乘停车场
//                        if(traveller.isBookingParkUser()){ //预约停车用户，直接停车
//                            //可预约泊位数量在预约的时候已经扣减，现在不用扣减
//                            traveller.setStartParkTime(m);
//                            targetParkingLot.getBookingUserParkList().add(traveller);
//                            traveller.setLastPassedNode(targetParkingLot.getNodeIdForCarMode());
//                            int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
//                            traveller.setNextNode(nextNode);
//                            traveller.getPath().add(traveller.getNextNode());
//                            Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
//                            traveller.setCurrentLink(nextLink);
//                            traveller.setLastLink(currentLink);
//                            traveller.setDistanceToNextNode(nextLink.getLength());
//                            traveller.setTrafficMode(nextLink.getTrafficMode());
////                            traveller.setFirstArriveToLink(true);
//                            traveller.setHasPark(true);
//                            traveller.setHasParkAndRide(true);
//                            traveller.setArriveParkingLot(false);
//                            System.out.println("到达停车场并完成停车:" + traveller);
//                        }else{ //非预约用户
//                            if(targetParkingLot.hasAvailableGeneralBerth()){ //有空余泊位
//                                traveller.setStartParkTime(m);
//                                targetParkingLot.addGeneralPark(traveller);
//                                traveller.setLastPassedNode(targetParkingLot.getNodeIdForCarMode());
//                                int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
//                                traveller.setNextNode(nextNode);
//                                traveller.getPath().add(traveller.getNextNode());
//                                Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
//                                traveller.setCurrentLink(nextLink);
//                                traveller.setLastLink(currentLink);
//                                traveller.setDistanceToNextNode(nextLink.getLength());
////                                traveller.setFirstArriveToLink(true);
//                                traveller.setHasPark(true);
//                                traveller.setHasParkAndRide(true);
//                                traveller.setArriveParkingLot(false);
//                                traveller.setTrafficMode(nextLink.getTrafficMode());
//                                System.out.println("完成换乘停车，前往下一个节点: " + traveller);
//                            }else{ //换乘停车场没有空余泊位，则比较两种出行方案，并确定下一个节点
//                                System.out.println("换乘停车场没有空余泊位");
//                                Queue<Integer> visitedParkingLots = traveller.getVisitedParkingLots();
//                                if(!visitedParkingLots.isEmpty() && (visitedParkingLots.peek() == targetParkingLot.getId())){
//                                    visitedParkingLots.poll(); //从头部删除，并添加到队列尾部，表示刚刚访问过该停车场
//                                }
//                                visitedParkingLots.offer(targetParkingLot.getId());
//                                int currentNode = targetParkingLot.getNodeIdForCarMode();
//                                int nextNode = compareAndGetNextNodeOfTravel(traveller, currentNode);
//                                if(traveller.getTargetParkingLot().getId() == targetParkingLot.getId()){ //选择当前停车场排队
//                                    continue;
//                                }else{
//                                    traveller.setLastPassedNode(currentNode);
//                                    traveller.setNextNode(nextNode);
//                                    traveller.getPath().add(traveller.getNextNode());
//                                    Link nextLink = getLink(traveller.getLastPassedNode(), traveller.getNextNode());
//                                    traveller.setCurrentLink(nextLink);
//                                    traveller.setLastLink(currentLink);
//                                    traveller.setDistanceToNextNode(nextLink.getLength());
//                                    traveller.setTrafficMode(nextLink.getTrafficMode());
//                                    traveller.setFirstArriveToLink(true);
//                                    traveller.setParkAndRideModeFail(true);
//                                    traveller.setChooseParkAndRide(false);
//                                    traveller.setHasPark(false);
//                                    traveller.setArriveParkingLot(false);
//                                    System.out.println("前往下一个节点: " + nextNode);
//                                }
//                            }
//                        }
//
//                    }else{ //自驾车出行到达目的地附近停车场
//                        if(traveller.isBookingParkUser()){ //预约停车用户，到达停车场直接停车
//                            //可预约泊位数量在预约的时候已经扣减，现在不用扣减
//                            traveller.setStartParkTime(m);
//                            targetParkingLot.getBookingUserParkList().add(traveller);
//                            traveller.setLastPassedNode(targetParkingLot.getNodeIdForCarMode());
//                            int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
//                            traveller.setNextNode(nextNode);
//                            traveller.getPath().add(traveller.getNextNode());
//                            Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
//                            traveller.setCurrentLink(nextLink);
//                            traveller.setLastLink(currentLink);
//                            traveller.setDistanceToNextNode(nextLink.getLength());
//                            traveller.setTrafficMode(nextLink.getTrafficMode());
////                            traveller.setFirstArriveToLink(true);
//                            traveller.setHasPark(true);
//                            traveller.setArriveParkingLot(false);
//                            System.out.println("到达停车场并完成停车:" + traveller);
//                        }else{ //普通停车用户
//                            if(targetParkingLot.hasAvailableGeneralBerth()){ //有空余泊位
//                                traveller.setStartParkTime(m);
////                                if(traveller.getStartParkTime() > traveller.getFirstArriveParkingLotTime()){
////                                    throw new RuntimeException("停车巡游时间：" + (traveller.getStartParkTime()-traveller.getFirstArriveParkingLotTime()));
////                                }
//                                targetParkingLot.addGeneralPark(traveller);
//                                traveller.setLastPassedNode(targetParkingLot.getNodeIdForWalkMode());
//                                int nextNode = getNextNodeAfterPark(targetParkingLot.getNodeIdForWalkMode(), traveller.getEndNode());
//                                traveller.setNextNode(nextNode);
//                                Link nextLink = getLink(traveller.getLastPassedNode(), nextNode);
//                                traveller.setCurrentLink(nextLink);
//                                traveller.setLastLink(currentLink);
//                                traveller.setDistanceToNextNode(nextLink.getLength());
//                                traveller.setTrafficMode(nextLink.getTrafficMode());
////                                traveller.setFirstArriveToLink(true);
//                                traveller.setHasPark(true);
//                                traveller.setArriveParkingLot(false);
//                                System.out.println("到达停车场并完成停车:" + traveller);
//                            }else{ //没有空余泊位
//                                System.out.println("停车场" + targetParkingLot.getId() + "没有泊位");
//                                if(traveller.isChooseQueueUp()){ //选择排队
//                                    traveller.setStartQueueTime(m);
//                                    traveller.setLastLink(traveller.getCurrentLink());
//                                    traveller.setQueuing(true);
//                                    traveller.setChooseQueueUp(false);
//                                    targetParkingLot.getWaitingQueue().offer(traveller);
//                                }else{
//                                    Queue<Integer> visitedParkingLots = traveller.getVisitedParkingLots();
//                                    if(!visitedParkingLots.isEmpty() && (visitedParkingLots.peek() == targetParkingLot.getId())){
//                                        visitedParkingLots.poll(); //从头部删除，并添加到队列尾部，表示刚刚访问过该停车场
//                                    }
//                                    visitedParkingLots.offer(targetParkingLot.getId());
//                                    int currentNode = targetParkingLot.getNodeIdForCarMode();
//                                    ParkingLot nextParkingLot = ParkSystem.selectNextParkingLot(traveller, currentNode);
//                                    if(nextParkingLot.getId() == targetParkingLot.getId()){ //选择当前停车场排队
//                                        continue;
//                                    }else{
//                                        traveller.setLastPassedNode(currentNode);
//                                        traveller.setTargetParkingLot(nextParkingLot);
//                                        traveller.setNextNode(getNextNodeOfCarTravel(traveller.getLastPassedNode(),nextParkingLot.getNodeIdForCarMode()));
//                                        traveller.getPath().add(traveller.getNextNode());
//                                        Link nextLink = getLink(traveller.getLastPassedNode(), traveller.getNextNode());
//                                        traveller.setCurrentLink(nextLink);
//                                        traveller.setLastLink(currentLink);
//                                        traveller.setDistanceToNextNode(nextLink.getLength());
//                                        traveller.setArriveParkingLot(false);
//                                        traveller.setFirstArriveToLink(true);
//                                        logger.info("前往下一个停车场：" + nextParkingLot);
//                                    }
//                                }
//
//                            }
//                        }
//                    }
//                }
//                //情况3 ：到达普通节点 --> 到达目的地 或 决定下一个节点
//                else{
//                    if(traveller.isHasPark()){ //已经完成停车
//                        if(traveller.getNextNode() == traveller.getEndNode()){ // 到达目的地 --> 出行结束
//                            traveller.setArrivalTime(m);
//                            traveller.setTravelFinished(true);
////                            if(traveller.isCommuter() && traveller.isHasParkAndRide()){
////                                throw new RuntimeException("通勤出行者到达目的地：" + traveller);
////                            }
//                            finishedTravellerMap.put(traveller.getId(), traveller);
//                            iterator.remove();
//                            System.out.println("到达目的地: " + traveller);
//                        }else{ //停车后未到达目的地 --> 前往下一个节点
//                            logger.info("停车后未到达目的地");
//                            int currentNode = traveller.getNextNode();
//                            traveller.setLastPassedNode(currentNode);
//                            int nextNodeOfPath = getNextNodeAfterPark(currentNode, traveller.getEndNode());
//                            traveller.setNextNode(nextNodeOfPath);
//                            Link nextLink = getLink(currentNode, nextNodeOfPath);
//                            if(nextLink == null){
//                                throw new RuntimeException("路段" + "currentNode:" + currentNode + "———nextNodeOfPath: " + nextNodeOfPath + "不存在");
//                            }
//                            traveller.setTrafficMode(nextLink.getTrafficMode());
//                            traveller.getPath().add(nextNodeOfPath);
//                            traveller.setLastLink(currentLink);
//                            traveller.setCurrentLink(nextLink);
//                            traveller.setFirstArriveToLink(true);
//                            traveller.setDistanceToNextNode(nextLink.getLength());
//                            System.out.println("前往下一个节点: " + nextNodeOfPath);
//                        }
//                    }else{ //未完成停车，正前往停车场途中
//                        System.out.println("未完成停车，正前往停车场途中" + traveller.toString());
//                        int currentNode = traveller.getNextNode();
//                        int nextNode = compareAndGetNextNodeOfTravel(traveller, currentNode);
//                        if(nextNode == -1 && traveller.getTargetParkingLot().getNodeIdForCarMode() == currentNode && traveller.isChooseQueueUp()){
//                            //说明选择当前途经停车场进行排队停车
//                            continue;
//                        }
//                        traveller.setLastPassedNode(currentNode);
//                        traveller.setNextNode(nextNode);
//                        if(currentNode == 18 && nextNode == -1){
//                            System.out.println("路段49的旅行速度：" + linkMap.get(49).getSpeed());
//                            System.out.println("路段49的流量：" + linkMap.get(49).getVolume());
//                        }
//                        Link nextLink = getLink(currentNode, nextNode);
//                        traveller.setTrafficMode(nextLink.getTrafficMode());
//                        traveller.getPath().add(nextNode);
//                        traveller.setLastLink(currentLink);
//                        traveller.setCurrentLink(nextLink);
//                        traveller.setDistanceToNextNode(nextLink.getLength());
//                        traveller.setFirstArriveToLink(true);
//                        System.out.println("前往下一个节点: " + nextNode);
//                    }
//                }
//
//                if(traveller.getPath().size() > linkNum/5){
//                    System.out.println(traveller);
//                    iterator.remove();
////                    throw new RuntimeException("出行者" + traveller.getId() + "的路径过长，存在异常：" + traveller);
//                }
//            }
//
//
//            /*---------- 步骤5：更新停车场停车列表 -----------*/
//            System.out.println("\n\n--------更新停车场停车列表----------");
//            for(int key : parkingLotMap.keySet()){
//                ParkingLot parkingLot = parkingLotMap.get(key);
//
//                //检查普通停车队列
//                List<Traveller> generalParkList = parkingLot.getGeneralParkList();
//                if(generalParkList.size() > 0){
//                    for(Iterator<Traveller> iterator = generalParkList.iterator(); iterator.hasNext();){
//                        Traveller traveller = iterator.next();
//                        if((m - traveller.getStartParkTime()) * stepSize  >= traveller.getParkTime() * 60){ //如果已经达到停车时长，则删除该车辆
//                            logger.info("停车场" + parkingLot.getId() + "已使用的普通泊位数量：" + parkingLot.getUsedGeneralBerth());
//                            parkingLot.decreaseUsedGeneralBerthByOne();
//                            iterator.remove();
//                        }
//                    }
//                }
//
//                //检查预约停车队列
//                List<Traveller> bookingParkList = parkingLot.getBookingUserParkList();
//                if(bookingParkList.size() > 0){
//                    for(int i=bookingParkList.size()-1; i>=0; i--){
//                        Traveller traveller = bookingParkList.get(i);
//                        if((m - traveller.getStartParkTime())*stepSize >= traveller.getParkTime() * 60){ //如果已经达到停车时长，则删除该车辆
//                            parkingLot.decreaseUsedBookableBerthByOne();
//                            bookingParkList.remove(traveller);
//                        }
//                    }
//                }
//
//                //检查排队队列
//                if(!parkingLot.getWaitingQueue().isEmpty()){
//                    Queue<Traveller> waitingQueue =  parkingLot.getWaitingQueue();
//                    if(parkingLot.hasAvailableGeneralBerth()){
//                        int berthNum = parkingLot.getGeneralBerth() - parkingLot.getUsedGeneralBerth();
//                        for(int i = 0; i<Math.min(berthNum, waitingQueue.size()); i++){
//                            Traveller traveller = waitingQueue.poll();
//                            parkingLot.addGeneralPark(traveller);
//                            traveller.setStartParkTime(m);
//                            traveller.setHasPark(true);
//                            traveller.setQueuing(false);
//                            traveller.setArriveParkingLot(false);
//                        }
//                    }
//                }
//
//            }
//
//
//            /*---------- 步骤6：数据统计 -----------*/
//            //统计路段相关数据
//            System.out.println("--------数据统计----------");
//            //统计停车场相关数据
//            for(int key : parkingLotMap.keySet()){
//                ParkingLot parkingLot = parkingLotMap.get(key);
//                totalBerthOccupy[parkingLot.getId()][m-1] = parkingLot.getBerthOccupancy();
//                usedGeneralBerthOc[parkingLot.getId()][m-1] = parkingLot.getGeneralBerthOccupancy();
//                usedBookableBerthOc[parkingLot.getId()][m-1] = parkingLot.getBookableBerthOccupancy();
//                parkFee[parkingLot.getId()][m-1] = parkingLot.getFee();
//            }
//        }
//        /*-——————————————————————————————————————————————迭代结束——————————————————————————————————————————————————————————————————*/
//
//        //统计出行者相关数据
//        double avgCarDistance = 0; //平均车公里数
//        double avgParkSearchTime = 0;
//        int parkSearchCount = 0;
//
//        totalCarDistance = 0;
//        totalMetroAndBusDistance = 0;
//        travelerTimeRecord = new int[idGenerator.get()];
//        for(int key : finishedTravellerMap.keySet()){
//            Traveller traveller = finishedTravellerMap.get(key);
//            travelerTimeRecord[traveller.getId()] = traveller.getArrivalTime() - traveller.getDepartureTime(); //统计旅行者旅行时间
//            if(!traveller.isHasParkAndRide()){
//                avgCruisingTimeRecord += (traveller.getStartParkTime() - traveller.getFirstArriveParkingLotTime());
//                drivingCount++;
//            }
//
//            if(traveller.getStartParkTime() - traveller.getFirstArriveParkingLotTime() > 0){
//                avgParkSearchTime += (traveller.getStartParkTime() - traveller.getFirstArriveParkingLotTime());
//                parkSearchCount++;
//            }
//            avgCarDistance += traveller.getCarDistance();
//            totalCarDistance += traveller.getCarDistance();
//            totalMetroAndBusDistance += traveller.getMetroAndBusDistance();
//        }
//        if(drivingCount != 0){
//            avgCruisingTimeRecord /= drivingCount;
//        }
//        avgCarDistance /= finishedTravellerMap.size();
//        double temp = totalMetroAndBusDistance / (totalCarDistance + totalMetroAndBusDistance);
//        double[] data = new double[5];
//        data[0]=  avgCarDistance;
//        data[1] = avgParkSearchTime / parkSearchCount;
//        data[2] = parkSearchCount * 1.0 / drivingCount;
//        data[3] = (finishedTravellerMap.size() - drivingCount) * 1.0 / finishedTravellerMap.size();
//        data[4] = totalMetroAndBusDistance / (totalMetroAndBusDistance + totalCarDistance);
//        System.out.println("\n\n*********** 仿真结束 ************************");
//        System.out.println("生成出行者人数： " + idGenerator.get());
//        System.out.println("完成出行人数：" + finishedTravellerMap.size());
//        System.out.println("驾车出行人数：" + drivingCount + " 占比 " + (drivingCount * 1.0 / finishedTravellerMap.size()));
//        System.out.println("停车换乘出行人数：" + (finishedTravellerMap.size()-drivingCount) + " 占比 " + ((finishedTravellerMap.size()-drivingCount) * 1.0 / finishedTravellerMap.size()));
//        System.out.println("巡游人数：" + parkSearchCount);
//        System.out.println("平均车里程：" + avgCarDistance);
//        System.out.println("公共交通分担率：" + totalMetroAndBusDistance / (totalCarDistance + totalMetroAndBusDistance));
//        System.out.println(Arrays.toString(data));
//
//        return temp;
//    }

    public static double totalCarDistance = 0; //车公里数
    public static double totalMetroAndBusDistance = 0; //公共交通公里数
    public static double[][] data = new double[7][11];

    //发车频率分析
//    public static void test1() {
//        int c = 0;
//        for(double i=0.4; i>=0.4; i -= 0.1){
//            metroDepartureInterval = 6 * 60;
//            busDepartureInterval = 15 * 60;
//            metroDepartureInterval *= i;
//            busDepartureInterval *= i;
//            simulate(c);
//            c++;
//        }
//    }

    //分析行程时间标准差
    public static void test2(){
        int c = 0;
        List<Double> l = new ArrayList<>();
        for(double i = 0; i<0.9; i+=0.1){
            CPT.car_standardDeviationRate = i;
            simulate(c);
            c++;
            l.add(i);
        }
    }
//
    //票价分析
//    public static void test3(){
//        int c = 0;
//        for(double i=1; i>=0; i -= 0.1){
//            metroTicketPrice = 4 * i;
//            busTicketPrice = 2 * i;
//            simulate(c);
//            c++;
//        }
//    }
//
//    //预约停车比例分析
//    public static void test4(){
//        int c = 0;
//        for(double i=0; i<=1; i += 0.1){
//            bookingTravellerPercentage = i * 100;
//            simulate(c);
//            c++;
//        }
//        for(int i=0; i<data.length; i++){
//            System.out.println(Arrays.toString(data[i]));
//        }
//    }


    public static void main(String[] args) throws IOException, WriteException {
//        simulate(0);
        test2();

    }

}
