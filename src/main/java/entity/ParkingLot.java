package entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ParkingLot {
    private int id;
    private int nodeIdForCarMode; //汽车网络上对应的节点id
    private int nodeIdForWalkMode; //步行网络上对应的节点id
    private int totalBerth; //泊位总数，普通泊位与可预约泊位之和
    private int generalBerth; //普通泊位总数
    private int usedGeneralBerth; //已使用的普通泊位数
    private int bookableBerth; //可预约的泊位总数
    private int usedBookableBerth; //已使用（包括预约的）的可预约的泊位数
    private double bookingFee; //预约费用
    private double fee; //每小时停车费用（元）
    private List<Traveller> generalParkList; //普通用户的停车列表
    private List<Traveller> bookingUserParkList; //预约用户的停车列表
    private Queue<Traveller> waitingQueue; //等待停车的队列
    private List<Integer> adjacentCarNodes; //相邻的汽车网络节点
    private boolean isParkAndRide = false; //是否为换乘停车场

    private static Logger logger = LoggerFactory.getLogger(ParkingLot.class);

    public ParkingLot() {
        init();
    }

    public ParkingLot(int id) {
        this.id = id;
        init();
    }

    private void init(){
//        this.fee = 10 + (new Random().nextDouble() - 0.5) * 5; //停车费为7.5--12.5元每小时
        this.fee = 10;
//        this.bookingFee = 6 + (new Random().nextDouble() - 0.5) * 4; //预约费为4--8元每小时
        this.bookingFee = 10; //预约费为10元每小时
        this.generalParkList = new ArrayList<>();
        this.bookingUserParkList = new ArrayList<>();
        this.waitingQueue = new LinkedList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNodeIdForCarMode() {
        return nodeIdForCarMode;
    }

    public void setNodeIdForCarMode(int nodeIdForCarMode) {
        this.nodeIdForCarMode = nodeIdForCarMode;
    }

    public int getNodeIdForWalkMode() {
        return nodeIdForWalkMode;
    }

    public void setNodeIdForWalkMode(int nodeIdForWalkMode) {
        this.nodeIdForWalkMode = nodeIdForWalkMode;
    }

    public int getTotalBerth() {
        return totalBerth;
    }

    public void setTotalBerth(int totalBerth) {
        this.totalBerth = totalBerth;
    }

    public int getGeneralBerth() {
        return generalBerth;
    }

    public void setGeneralBerth(int generalBerth) {
        this.generalBerth = generalBerth;
    }

    public int getUsedGeneralBerth() {
        return usedGeneralBerth;
    }

    public void setUsedGeneralBerth(int usedGeneralBerth) {
        if(usedGeneralBerth < 0){
            throw new RuntimeException("泊位数量为负数：" + usedGeneralBerth);
        }
        this.usedGeneralBerth = usedGeneralBerth;
//        logger.info("停车场" + this.id + "已使用的普通泊位：" + this.usedGeneralBerth);
    }

    public int getBookableBerth() {
        return bookableBerth;
    }

    public void setBookableBerth(int bookableBerth) {
        this.bookableBerth = bookableBerth;
    }

    public int getUsedBookableBerth() {
        return usedBookableBerth;
    }

    public void setUsedBookableBerth(int usedBookableBerth) {
        this.usedBookableBerth = usedBookableBerth;
//        logger.info("停车场" + this.id + "已使用的预约泊位：" + this.usedGeneralBerth);
    }

    public double getBookingFee() {
        return bookingFee;
    }

    public void setBookingFee(double bookingFee) {
        this.bookingFee = bookingFee;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public List<Traveller> getGeneralParkList() {
        return generalParkList;
    }

    public void setGeneralParkList(List<Traveller> generalParkList) {
        this.generalParkList = generalParkList;
    }

    public List<Traveller> getBookingUserParkList() {
        return bookingUserParkList;
    }

    public void setBookingUserParkList(List<Traveller> bookingUserParkList) {
        this.bookingUserParkList = bookingUserParkList;
    }

    public Queue<Traveller> getWaitingQueue() {
        return waitingQueue;
    }

    public void setWaitingQueue(Queue<Traveller> waitingQueue) {
        this.waitingQueue = waitingQueue;
    }

    public List<Integer> getAdjacentCarNodes() {
        return adjacentCarNodes;
    }

    public void setAdjacentCarNodes(List<Integer> adjacentCarNodes) {
        this.adjacentCarNodes = adjacentCarNodes;
    }

    public boolean isParkAndRide() {
        return isParkAndRide;
    }

    public void setParkAndRide(boolean parkAndRide) {
        isParkAndRide = parkAndRide;
    }

    //返回停车场能接受的最大排队长度
    public int getMaxQueueLength() {
        return (int)(generalBerth * 0.2);
    }

    //普通泊位的占有率
    public double getGeneralBerthOccupancy(){
        if(generalBerth != 0){
            return (double) usedGeneralBerth / generalBerth;
        }else{
            return 1;
        }
    }

    //可预约泊位的占有率
    public double getBookableBerthOccupancy(){
        if(bookableBerth != 0){
            return (double) usedBookableBerth / bookableBerth;
        }else{
            return 1;
        }
    }


    public double getBerthOccupancy(){
        return (double)(usedGeneralBerth + usedBookableBerth) / totalBerth;
    }

    /**
     * 是否还有可使用的预约泊位
     * @return
     */
    public boolean hasAvailableBookableBerth(){
        return this.usedBookableBerth < this.bookableBerth;
    }

    /**
     * 是否还有可使用的普通泊位
     * @return
     */
    public boolean hasAvailableGeneralBerth(){
        return this.usedGeneralBerth < this.generalBerth;
    }


    /**
     * 添加预约停车者
     * @param traveller
     * @return 可用的预约泊位
     */
    public int addBookingPark(Traveller traveller){
        this.usedBookableBerth += 1;
        this.bookingUserParkList.add(traveller);
//        logger.info("停车场" + this.id + "已使用的预约泊位：" + this.usedGeneralBerth);
        return this.bookableBerth - this.usedBookableBerth;
    }

    /**
     * 添加普通停车者
     * @param traveller
     * @return 可用的普通泊位
     */
    public int addGeneralPark(Traveller traveller){
        addUsedGeneralBerthByOne();
        this.generalParkList.add(traveller);
//        logger.info("停车场" + this.id + "已使用的普通泊位：" + this.usedGeneralBerth);
        return this.generalBerth - this.usedGeneralBerth;
    }


    public boolean addUsedBookableBerthByOne(){
        if(this.usedBookableBerth < this.bookableBerth){
            this.usedBookableBerth += 1;
            return true;
        }
//        logger.info("停车场" + this.id + "可预约泊位已用完：" + this.usedBookableBerth + "/" + this.bookableBerth);
        return false;
    }

    public boolean decreaseUsedBookableBerthByOne(){
        if(this.usedBookableBerth > 0){
            this.usedBookableBerth -= 1;
            return true;
        }
//        logger.info("停车场" + this.id + "已使用的可预约泊位为0：" + this.usedBookableBerth + "/" + this.bookableBerth);
        return false;
    }

    public boolean addUsedGeneralBerthByOne(){
        if(this.usedGeneralBerth < this.generalBerth){
            this.usedGeneralBerth += 1;
            return true;
        }

//        logger.info("停车场" + this.id + "普通泊位已用完：" + this.usedGeneralBerth + "/" + this.generalBerth);
        return false;
    }

    public boolean decreaseUsedGeneralBerthByOne(){
        if(this.usedGeneralBerth > 0){
            this.usedGeneralBerth -= 1;
            return true;
        }
//        logger.info("停车场" + this.id + "已使用的普通泊位为0：" + this.usedGeneralBerth + "/" + this.generalBerth);
        return false;
    }

    @Override
    public String toString() {
        return "ParkingLot{" +
                "id=" + id +
                ", totalBerth=" + totalBerth +
                ", usedGeneralBerth=" + usedGeneralBerth +
                ", fee=" + fee +
                ", generalParkList=" + generalParkList.size() +
                '}';
    }
}
