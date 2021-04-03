package entity;

import util.MyUtils;
import util.TrafficMode;

import java.util.*;

public class Traveller {
    private int id; //出行者id
    private TrafficMode trafficMode; //当前交通方式
    private int departureTime; //从起点出发时刻
    private int arrivalTime = -1; //到达终点时刻
    private int startNode; //起点
    private int endNode; //终点
    private int lastPassedNode; //刚经过的节点
    private int nextNode; //下一个节点
    private double distanceToNextNode; //到下一个节点（或目标点停车场）的距离
    private List<Integer> path; //记录路径
    private double carDistance = 0.0; //汽车行驶距离（米）
    private double metroAndBusDistance = 0.0; //公共交通行驶距离（米）
    private double walkDistance = 0.0; //步行距离（米）
    private double start2endDistance = 0.0; //起点到终点距离（米）
    private Link currentLink = null; //当前所在路段
    private Link lastLink = null; //上一条经过的路段
    private int parkTime; //停车时长（分钟）
    private int startParkTime; //开始停车时刻(分钟)，该值减去firstArriveParkingLotTime的值可得巡游时间
    private int startQueueTime = -1; //开始排队时间
    private int firstArriveParkingLotTime; //第一次到达停车场时刻（未必有停车位）
    private ParkingLot targetParkingLot; //目标停车场
    private Deque<Integer> visitedParkingLots; //已经访问过的停车场id
    private boolean arriveParkingLot = false; //是否已经到达停车场
    private boolean firstArriveToLink = false; //是否第一次到达当前路段(用于更新路段流量)
    private boolean travelFinished = false; //出行是否结束
    private boolean chooseParkAndRide = false; //当前方案对比中是否选择停车换乘模式
    private boolean hasParkAndRide = false; //是否已经停车换乘模式
    private boolean hasPark = false; //是否已经停车
    private boolean parkAndRideModeFail = false; //停车换乘模式失败（比如换乘站点停车场已满），选择驾车模式
    private boolean highIncome = false; //是否为高收入者
    private boolean commuter = false; //是否通勤出行
    private boolean bookingParkUser = false; //是否为预约停车用户
    private boolean hasBookedBerth = false; //是否已预约泊位
    private boolean chooseQueueUp = false; //是否选择去停车场排队
    private boolean queuing = false; //是否正在停车场排队



    public Traveller(){
        init();
    }

    public Traveller(int id) {
        this.id = id;
        init();
    }

    //初始化
    private void init(){
        this.parkTime = (int)Math.ceil(MyUtils.normallyDistribution(120, 60));
        if(this.parkTime < 1){
            this.parkTime = 1;
        }
        this.path = new ArrayList<>();
        this.visitedParkingLots = new LinkedList<>();
    }

    public ParkingLot getTargetParkingLot() {
        return targetParkingLot;
    }

    public void setTargetParkingLot(ParkingLot targetParkingLot) {
        this.targetParkingLot = targetParkingLot;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public TrafficMode getTrafficMode() {
        return trafficMode;
    }

    public void setTrafficMode(TrafficMode trafficMode) {
        this.trafficMode = trafficMode;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getStartNode() {
        return startNode;
    }

    public void setStartNode(int startNode) {
        this.startNode = startNode;
    }

    public int getEndNode() {
        return endNode;
    }

    public void setEndNode(int endNode) {
        this.endNode = endNode;
    }

    public int getLastPassedNode() {
        return lastPassedNode;
    }

    public void setLastPassedNode(int lastPassedNode) {
        this.lastPassedNode = lastPassedNode;
    }

    public int getNextNode() {
        return nextNode;
    }

    public void setNextNode(int nextNode) {
        this.nextNode = nextNode;
    }

    public double getDistanceToNextNode() {
        return distanceToNextNode;
    }

    public void setDistanceToNextNode(double distanceToNextNode) {
        this.distanceToNextNode = distanceToNextNode;
    }

    public List<Integer> getPath() {
        return path;
    }

    public void setPath(List<Integer> path) {
        this.path = path;
    }

    public double getCarDistance() {
        return carDistance;
    }

    public void setCarDistance(double carDistance) {
        this.carDistance = carDistance;
    }

    public double getMetroAndBusDistance() {
        return metroAndBusDistance;
    }

    public void setMetroAndBusDistance(double metroAndBusDistance) {
        this.metroAndBusDistance = metroAndBusDistance;
    }

    public double getWalkDistance() {
        return walkDistance;
    }

    public void setWalkDistance(double walkDistance) {
        this.walkDistance = walkDistance;
    }

    public double getStart2endDistance() {
        return start2endDistance;
    }

    public void setStart2endDistance(double start2endDistance) {
        this.start2endDistance = start2endDistance;
    }

    public Link getCurrentLink() {
        return currentLink;
    }

    public void setCurrentLink(Link currentLink) {
        this.currentLink = currentLink;
    }

    public Link getLastLink() {
        return lastLink;
    }

    public void setLastLink(Link lastLink) {
        this.lastLink = lastLink;
    }

    public int getParkTime() {
        return parkTime;
    }

    public void setParkTime(int parkTime) {
        this.parkTime = parkTime;
    }

    public int getStartParkTime() {
        return startParkTime;
    }

    public void setStartParkTime(int startParkTime) {
        this.startParkTime = startParkTime;
    }

    public int getStartQueueTime() {
        return startQueueTime;
    }

    public void setStartQueueTime(int startQueueTime) {
        this.startQueueTime = startQueueTime;
    }

    public int getFirstArriveParkingLotTime() {
        return firstArriveParkingLotTime;
    }

    public void setFirstArriveParkingLotTime(int firstArriveParkingLotTime) {
        this.firstArriveParkingLotTime = firstArriveParkingLotTime;
    }

    public Deque<Integer> getVisitedParkingLots() {
        return visitedParkingLots;
    }

    public void setVisitedParkingLots(Deque<Integer> visitedParkingLots) {
        this.visitedParkingLots = visitedParkingLots;
    }

    public boolean isFirstArriveToLink() {
        return firstArriveToLink;
    }

    public void setFirstArriveToLink(boolean firstArriveToLink) {
        this.firstArriveToLink = firstArriveToLink;
    }

    public boolean isArriveParkingLot() {
        return arriveParkingLot;
    }

    public void setArriveParkingLot(boolean arriveParkingLot) {
        this.arriveParkingLot = arriveParkingLot;
    }

    public boolean isTravelFinished() {
        return travelFinished;
    }

    public void setTravelFinished(boolean travelFinished) {
        this.travelFinished = travelFinished;
    }

    public boolean isChooseParkAndRide() {
        return chooseParkAndRide;
    }

    public void setChooseParkAndRide(boolean chooseParkAndRide) {
        this.chooseParkAndRide = chooseParkAndRide;
    }

    public boolean isHasParkAndRide() {
        return hasParkAndRide;
    }

    public void setHasParkAndRide(boolean hasParkAndRide) {
        this.hasParkAndRide = hasParkAndRide;
    }

    public boolean isHasPark() {
        return hasPark;
    }

    public void setHasPark(boolean hasPark) {
        this.hasPark = hasPark;
    }

    public boolean isParkAndRideModeFail() {
        return parkAndRideModeFail;
    }

    public void setParkAndRideModeFail(boolean parkAndRideModeFail) {
        this.parkAndRideModeFail = parkAndRideModeFail;
    }

    public boolean isHighIncome() {
        return highIncome;
    }

    public void setHighIncome(boolean highIncome) {
        this.highIncome = highIncome;
    }

    public boolean isCommuter() {
        return commuter;
    }

    public void setCommuter(boolean commuter) {
        this.commuter = commuter;
    }

    public boolean isBookingParkUser() {
        return bookingParkUser;
    }

    public void setBookingParkUser(boolean bookingParkUser) {
        this.bookingParkUser = bookingParkUser;
    }

    public boolean isHasBookedBerth() {
        return hasBookedBerth;
    }

    public void setHasBookedBerth(boolean hasBookedBerth) {
        this.hasBookedBerth = hasBookedBerth;
    }

    public boolean isChooseQueueUp() {
        return chooseQueueUp;
    }

    public void setChooseQueueUp(boolean chooseQueueUp) {
        this.chooseQueueUp = chooseQueueUp;
    }

    public boolean isQueuing() {
        return queuing;
    }

    public void setQueuing(boolean queuing) {
        this.queuing = queuing;
    }

    @Override
    public String toString() {
        return "Traveller{" +
                "id=" + id +
                ", trafficMode=" + trafficMode +
                ", startNode=" + startNode +
                ", endNode=" + endNode +
                ", lastNode=" + lastPassedNode +
                ", nextNode=" + nextNode +
                ", distanceToNextNode=" + distanceToNextNode +
                ", metroAndBusDistance=" + metroAndBusDistance +
                ", path=" + path +
                ", currentLink=" + currentLink +
                ", targetParkingLot=" + targetParkingLot +
                ", arriveParkingLot=" + arriveParkingLot +
                ", hasPark=" + hasPark +
                ", bookingParkUser=" + bookingParkUser +
                ", commuter=" + commuter +
                ", chooseParkAndRide=" + chooseParkAndRide +
                '}';
    }

    public static void main(String[] args) {
        Traveller traveller = new Traveller(1);
        traveller.getVisitedParkingLots().offer(0);
        System.out.println(traveller.getVisitedParkingLots().contains(0));
    }
}
