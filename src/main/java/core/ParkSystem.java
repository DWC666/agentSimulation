package core;

import entity.Link;
import entity.ParkingLot;
import entity.Traveller;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.CPT;
import util.DijkstraAlgorithm;
import util.MyUtils;

import java.util.*;

/**
 * 停车系统
 */
public class ParkSystem {
    private static final double cruiseTimeOfFreeFlow = 1; //自由流状态下的停车巡游时间（分钟）
    private static final double alpha = 2; //停车巡游时间公式的参数
    private static final double beta = 4.03; //停车巡游时间公式的参数
    private static final double w1 = 0.5; //停车场占有率权重
    private static final double w2 = 1 - w1; //道路饱和度权重
    private static final double fixedFee = 10; //基本预约费用
    private static final double feeFactor = 0.5; //时间-费用系数

    public static final double endNodeParkSearchRound = 1000; //终点处停车场搜寻范围（米）
    public static final double bookingFeeUpdateStep = 1; //预约费更新频率
    private static final int[] parkingLotIDs = {2,3,4,5}; //需要更新预约费用的停车场编号
    private static final int INT_MAX = 9999999;

    //用于记录数据
    public static List<Double> carModeCost = new ArrayList<>();
    public static List<Double> prModeCost = new ArrayList<>();
    public static List<Double> carModeCPV = new ArrayList<>();
    public static List<Double> prModeCPV = new ArrayList<>();

    private static Logger logger = LoggerFactory.getLogger(ParkSystem.class);

    /**
     * 计算停车场相连的汽车路段的平均饱和度
     * @param p
     * @return
     */
    public static double adjacentLinksSaturation(ParkingLot p){
        List<Integer> adjacentCarNodes = p.getAdjacentCarNodes();
        List<Link> links = new ArrayList<>();
        for(int i : adjacentCarNodes){
            links.add(SimulationMain.getLink(i, p.getNodeIdForCarMode()));
            links.add(SimulationMain.getLink(p.getNodeIdForCarMode(), i));
        }
        if(links.size() == 0){
            return 0;
        }
        double saturation = 0.0;
        for(Link l : links){
            saturation += l.getSaturation();
        }
        return saturation / links.size();
    }


    /**
     * 更新指定停车场的预约费用
     * @param m 当前时间
     * @return
     */
    public static void updateBookingFee(int m){
        if(m % bookingFeeUpdateStep == 0){
            for(int i : parkingLotIDs){
                ParkingLot parkingLot = SimulationMain.parkingLotMap.get(i);
                double bookingFee = getFee2(parkingLot.getBerthOccupancy());
                parkingLot.setBookingFee(bookingFee);
            }
        }
    }


    public static void updateParkFee(int m){
        if(m % bookingFeeUpdateStep == 0){
            for(int i : parkingLotIDs){
                ParkingLot parkingLot = SimulationMain.parkingLotMap.get(i);
                double fee = getFee(parkingLot.getBerthOccupancy(), adjacentLinksSaturation(parkingLot));
                parkingLot.setFee(fee);
            }
        }
    }


    /**
     * 计算停车预约费
     * @param occupancy 停车场泊位占有率
     * @param saturation 停车场附近路段饱和度
     * @return
     */
    public static double getFee(double occupancy, double saturation){
        double v = 1 + alpha * Math.pow(w1 * occupancy + w2 * saturation, beta);
        return fixedFee * v;
    }

    public static double getFee2(double occupancy){
        double v = 1 + alpha * Math.pow(occupancy, beta);
        return fixedFee * v;
    }


    public static double cruiseTime(double occupancy, double saturation){
        double cruiseTime = 1 + alpha * Math.pow(w1 * occupancy + w2 * saturation, beta);
        return cruiseTime;
    }

    public static double cruiseTime0(double occupancy){
        double cruiseTime = cruiseTimeOfFreeFlow * (1 + alpha * Math.pow(occupancy, beta));
        return cruiseTime;
    }

    /**
     * 比较自驾车方案和停车换乘方案，为预约停车用户预约泊位并返回下一个节点
     * @param traveller
     * @param currentNode
     * @return
     */
    public static int bookBerthAndGetNextNode(Traveller traveller, int currentNode){
        if(traveller.isCommuter()){
            return bookBerthAndGetNextNodeByCPT(traveller, currentNode);
        }

        logger.info("比较出行方案：" + "出行者id=" + traveller.getId() + ", 当前节点=" + currentNode + ", 目的节点=" + traveller.getEndNode());
        double[][] costMatrix = SimulationMain.calculateAndGetTotalCostMatrix(traveller.isHighIncome(), traveller.isBookingParkUser());

        //计算驾车方案的成本
        List<ParkingLot> candidateParkingLot1 = candidateParkingLot(traveller, currentNode, false);
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
        logger.info("    预约用户自驾车方案： 路径=" + shortestPath1 + ", 成本=" + driveModeCost);

        //方案2成本，即开车前往最近的地铁站换乘
        List<ParkingLot> candidateParkingLot2 = candidateParkingLot(traveller, currentNode, true);
        double parkAndRideCost = Double.MAX_VALUE;
        ParkingLot targetTransferParkingLot = null;
        List<Integer> shortestPath2 = null;
        //当前节点开车到目的地的最短距离
        double current2endDistance = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, currentNode, traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost;
        //判断当前节点到目的地的距离是否达到停车换乘阈值
        if(current2endDistance / traveller.getStart2endDistance() >= SimulationMain.parkAndRideThreshold){
            for(ParkingLot curParkingLot : candidateParkingLot2) {
                DijkstraAlgorithm.DijkstraResult<Double> dijkstraResult = DijkstraAlgorithm.dijkstra4ParkAndRide(costMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
                if(dijkstraResult.cost < parkAndRideCost){
                    parkAndRideCost = dijkstraResult.cost;
                    targetTransferParkingLot = curParkingLot;
                    shortestPath2 = dijkstraResult.path;
                }
            }
        }
        logger.info("    预约用户停车换乘方案： 路径=" + shortestPath2 + ", 成本=" + parkAndRideCost);

        if(targetParkingLot == null && targetTransferParkingLot == null){
            logger.info(traveller.getId() + "预约泊位失败");
            traveller.setBookingParkUser(false);
            return -1;
        }
        if(driveModeCost < Double.MAX_VALUE){
            carModeCost.add(driveModeCost);
        }
        if(parkAndRideCost < Double.MAX_VALUE){
            prModeCost.add(parkAndRideCost);
        }


        //比较两种方案的成本
        if(-driveModeCost >= -parkAndRideCost){
            logger.info("    选择自驾车");
            logger.info("目标停车场：" + targetParkingLot.getId());
            boolean result = targetParkingLot.addUsedBookableBerthByOne();
            if(!result){
                throw new RuntimeException("没有空闲的预约泊位");
            }
            traveller.setTargetParkingLot(targetParkingLot);
            traveller.setChooseParkAndRide(false);

        }else{
            logger.info("    选择停车换乘, 目标停车场：" + targetTransferParkingLot.getId());
            traveller.setChooseParkAndRide(true);
            boolean result = targetTransferParkingLot.addUsedBookableBerthByOne();
            if(!result){
                throw new RuntimeException("没有空闲的预约泊位");
            }
            traveller.setTargetParkingLot(targetTransferParkingLot);
        }

        return traveller.getStartNode() == 20 ? 0 : 19;
    }


    /**
     * 比较自驾车方案和停车换乘方案，使用累积前景理论为预约停车用户预约泊位并返回下一个节点
     * @param traveller
     * @param currentNode
     * @return
     */
    public static int bookBerthAndGetNextNodeByCPT(Traveller traveller, int currentNode){
        double[][] costMatrix = SimulationMain.travelTimeMatrix;
        System.out.println("使用CPT进行路径选择");
        double[] referencePoints = SimulationMain.getReferencePoint(traveller, currentNode);

        //方案1, 即开车前往目的地
        List<ParkingLot> candidateParkingLot1 = candidateParkingLot(traveller, currentNode, false);
//        double[] referencePoints = SimulationMain.getReferencePoint(traveller, currentNode, candidateParkingLot1,false);
        ParkingLot targetParkingLot = null;
        double driveModeCpv = -Double.MAX_VALUE;
        List<Integer> shortestPath1 = null;
        for(ParkingLot curParkingLot : candidateParkingLot1){
            //开车到停车场过程
            DijkstraAlgorithm.DijkstraResult<Double> drive2park = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), SimulationMain.notCarNodes);
            DijkstraAlgorithm.DijkstraResult<Double> lengthOfDrive2park = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), SimulationMain.notCarNodes);

            Set<Integer> carAndPublicTrafficNodes = new HashSet<>();
            carAndPublicTrafficNodes.addAll(SimulationMain.publicTrafficNodes);
            carAndPublicTrafficNodes.addAll(SimulationMain.carNodes);
            //从停车场步行到终点
            DijkstraAlgorithm.DijkstraResult<Double> walk2end = DijkstraAlgorithm.dijkstra(costMatrix, curParkingLot.getNodeIdForCarMode(), traveller.getEndNode(), carAndPublicTrafficNodes);
            List<Integer> mergePath = DijkstraAlgorithm.mergePath(drive2park.path, walk2end.path);
            double currency = (lengthOfDrive2park.cost * SimulationMain.fuelCostPerMeter + curParkingLot.getFee() + curParkingLot.getBookingFee()) * SimulationMain.currency2timeFactor; //货币转换成时间
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


        //方案2成本，即开车前往最近的换乘站换乘
        List<ParkingLot> candidateParkingLot2 = candidateParkingLot(traveller, currentNode, true);
//        double[] referencePoints2 = SimulationMain.getReferencePoint(traveller, currentNode, candidateParkingLot2,true);
        double parkAndRideCpv = -Double.MAX_VALUE;
        ParkingLot transferParkingLot = null;
        List<Integer> shortestPath2 = null;
        //当前节点开车到目的地的最短距离
        double current2endDistance = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, currentNode, traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost;
        //判断当前节点到目的地的距离是否达到停车换乘阈值
        if(current2endDistance / traveller.getStart2endDistance() >= SimulationMain.parkAndRideThreshold){
            for(ParkingLot curParkingLot : candidateParkingLot2) {
                //当前节点开车到换乘停车场的最短距离
                double current2parkDistance = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), SimulationMain.notCarNodes).cost;
                DijkstraAlgorithm.DijkstraResult<Double> dijkstraResult = DijkstraAlgorithm.dijkstra4ParkAndRide(costMatrix, currentNode, curParkingLot.getNodeIdForCarMode(), traveller.getEndNode());
                //如果路径中没有真正乘坐公共交通，则不考虑该路线
                if(!DijkstraAlgorithm.takePublicTraffic(dijkstraResult.path)){
                    continue;
                }
                double publicTrafficFee = SimulationMain.getPublicTrafficFee(dijkstraResult.path);
                double currency = (current2parkDistance * SimulationMain.fuelCostPerMeter + curParkingLot.getFee() +curParkingLot.getBookingFee() + publicTrafficFee) * SimulationMain.currency2timeFactor; //货币转换成时间
                NormalDistribution normalDistribution = CPT.hyperPathNormalDistribution(dijkstraResult.path);
                double timeCpv = CPT.cpv(-1 * referencePoints[0], normalDistribution);
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

        if(targetParkingLot == null && transferParkingLot == null){
            logger.info(traveller.getId() + "预约泊位失败");
            traveller.setBookingParkUser(false);
            return -1;
        }

        if(driveModeCpv > -Double.MAX_VALUE){
            carModeCPV.add(driveModeCpv);
        }
        if(parkAndRideCpv > -Double.MAX_VALUE){
            prModeCPV.add(parkAndRideCpv);
        }

        //选择累积前景值较大的
        if(driveModeCpv >= parkAndRideCpv){
            logger.info("    选择自驾车，停车场为" + targetParkingLot.getId());
            traveller.setTargetParkingLot(targetParkingLot);
            boolean result = targetParkingLot.addUsedBookableBerthByOne();
            if(!result){
                throw new RuntimeException("没有空闲的预约泊位");
            }
        }else{
            logger.info("    选择停车换乘，停车场为" + transferParkingLot.getId());
            traveller.setChooseParkAndRide(true);
            traveller.setTargetParkingLot(transferParkingLot);
            boolean result = transferParkingLot.addUsedBookableBerthByOne();
            if(!result){
                throw new RuntimeException("没有空闲的预约泊位");
            }
        }
        return traveller.getStartNode() == 20 ? 0 : 19;
    }


    /**
     * 在所有停车场都满了的情况下，寻找排队长度最短且不是刚访问过的停车场
     * @param traveller
     * @param currentNode
     * @return
     */
    public static ParkingLot selectParkingLotWhenAllIsFull(Traveller traveller, int currentNode){
        ParkingLot targetParkingLot = null;
        int minQueue = INT_MAX;

        Set<Integer> carAndPublicTrafficNodes = new HashSet<>();
        carAndPublicTrafficNodes.addAll(SimulationMain.publicTrafficNodes);
        carAndPublicTrafficNodes.addAll(SimulationMain.carNodes);
        for(int k : SimulationMain.parkingLotMap.keySet()){
            ParkingLot p = SimulationMain.parkingLotMap.get(k);
            logger.info("判断停车场" + p.getId());
            //停车场到终点的步行距离
            double walk2endDistance = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, p.getNodeIdForWalkMode(), traveller.getEndNode(), carAndPublicTrafficNodes).cost;
            if(walk2endDistance > 1000){ //步行到目的地的距离大于1千米时则不考虑该停车场
                continue;
            }

            //选择排队长度最短的停车场
            if(p.getWaitingQueue().size() < minQueue){
                minQueue = p.getWaitingQueue().size();
                targetParkingLot = p;
            }
        }
        logger.info("选择停车场：" + targetParkingLot.getId());
        traveller.setTargetParkingLot(targetParkingLot);
        traveller.setChooseQueueUp(true);
        traveller.setChooseParkAndRide(false);
        return targetParkingLot;
    }

    /**
     * 获取候选停车场
     * @param traveller
     * @param currentNode
     * @param parkAndRide 为true时，表示获取符合筛选要求的换乘停车场
     * @return
     */
    public static List<ParkingLot> candidateParkingLot(Traveller traveller, int currentNode, boolean parkAndRide){
        List<ParkingLot> candidateParkingLot = new ArrayList<>();
        if(parkAndRide){
            //当前节点到目的地的最短距离
            double start2endDistance = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, currentNode, traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost;

            
            for(Iterator<Map.Entry<Integer, ParkingLot>> iterator = SimulationMain.parkingLotMap.entrySet().iterator(); iterator.hasNext();){
                Map.Entry<Integer, ParkingLot> next = iterator.next();
                ParkingLot p = next.getValue();
                if(p.getNodeIdForCarMode() == currentNode || !p.isParkAndRide()){
                    continue;
                }
                if(traveller.isBookingParkUser() && !p.hasAvailableBookableBerth()){
                    continue;
                }
                if(!traveller.isBookingParkUser() && !p.hasAvailableGeneralBerth()){
                    continue;
                }
                //起点开车到换乘停车场的最短距离
                double start2parkingLotDistance = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes).cost;
                if(start2parkingLotDistance >= start2endDistance){ //到换乘停车场的距离大于到目的地的距离时则不考虑该停车场
                    continue;
                }
                candidateParkingLot.add(p);
            }
        }else{
            Set<Integer> carAndPublicTrafficNodes = new HashSet<>();
            carAndPublicTrafficNodes.addAll(SimulationMain.publicTrafficNodes);
            carAndPublicTrafficNodes.addAll(SimulationMain.carNodes);
            for(int k : SimulationMain.parkingLotMap.keySet()){
                ParkingLot p = SimulationMain.parkingLotMap.get(k);
                if(p.getNodeIdForCarMode() == currentNode){
                    continue;
                }
                if(traveller.isBookingParkUser() && !p.hasAvailableBookableBerth()){
                    continue;
                }
                if(!traveller.isBookingParkUser() && !p.hasAvailableGeneralBerth()){
                    continue;
                }

                //停车场到终点的步行距离
                double walk2endDistance = DijkstraAlgorithm.dijkstra(SimulationMain.lengthMatrix, p.getNodeIdForCarMode(), traveller.getEndNode(), carAndPublicTrafficNodes).cost;
                if(walk2endDistance > endNodeParkSearchRound){ //步行到目的地的距离大于1千米时则不考虑该停车场
                    continue;
                }
                candidateParkingLot.add(p);
            }
        }
        return candidateParkingLot;
    }


    /**
     *  停车失败后，在剩下的停车场中选择最佳的停车场
     * @return
     */
    public static ParkingLot selectNextParkingLot(Traveller traveller, int currentNode){
        System.out.println("停车巡游，选择下一个目标停车场：");
        Deque<Integer> visitedParkingLots = traveller.getVisitedParkingLots();
        ParkingLot targetParkingLot = null;
        double minCost = Double.MAX_VALUE;

        if(visitedParkingLots.size() == SimulationMain.parkingLotNum){ //如果所有停车场都去过
            targetParkingLot = SimulationMain.parkingLotMap.get(visitedParkingLots.poll()); //弹出访问队列的第一个元素，即最久没有被访问的元素，作为下一次的目标停车场
        }else{
            double[][] costMatrix = SimulationMain.calculateAndGetTotalCostMatrix(traveller.isHighIncome(), traveller.isBookingParkUser());
            List<ParkingLot> parkingLots = candidateParkingLot(traveller, currentNode, false);
            for(ParkingLot p : parkingLots){
                if(visitedParkingLots != null && visitedParkingLots.getLast() == p.getId()){ //跳过刚刚访问过的停车场
                    continue;
                }

                //当前节点到停车场
                double driveCost = DijkstraAlgorithm.dijkstra(costMatrix, currentNode, p.getNodeIdForCarMode(), SimulationMain.notCarNodes).cost;
                //停车场步行到目的地
                double walkCost = DijkstraAlgorithm.dijkstra(costMatrix, p.getNodeIdForWalkMode(), traveller.getEndNode(), SimulationMain.publicTrafficNodes).cost;
                //根据开车到停车场成本+步行到目的地的时间（分钟）+ 停车费用 来选择最佳停车场
                double totalCost = driveCost + walkCost;
                System.out.println("    停车场" + p.getId() + "的成本：" + totalCost);
                if(totalCost < minCost){
                    minCost = totalCost;
                    targetParkingLot = p;
                }
            }
        }

        //如果所有停车场都没有泊位
        if(targetParkingLot == null){
            return selectParkingLotWhenAllIsFull(traveller, currentNode);
        }

        return targetParkingLot;
    }


    public static double arrayAverage(List<Double> list){
        double sum = 0;
        for(double n : list){
            sum += n;
        }
        return sum/list.size();
    }


    public static void test(){
        double[] f = new double[11];
        double n = 0;
        for(int j=0; j<11; j++){
            f[j] = cruiseTime0(n);
            n += 0.1;
        }
        System.out.println(Arrays.toString(f));;
    }

    public static void test1(){
        double[][] f = new double[11][11];
        double m = 0 , n = 0;
        for(int i=0; i<11; i++){
            n = 0;
            for(int j=0; j<11; j++){
                System.out.println("m:" + m + " n:" + n);
                System.out.println("i:" + i + " j:" + j);
                f[i][j] = cruiseTime(m, n);
                n += 0.1;
            }
            m += 0.1;
        }
//        ExcelUtil.writeDataToExcel(f);
        MyUtils.printMatrix(f);
    }

    public static void main(String[] args) {
        System.out.println( getFee(0.24, 0.26));
    }



}
