package entity;

import util.MyUtils;
import util.TrafficMode;

public class Link {
    private int id; //路段id
    private int fromNode; //路段起点id
    private int toNode; //路段终点id
    private double length; //路段长度
    private TrafficMode trafficMode; //路段所属交通方式
    private double capacity; //路段通行能力
    private double volume; //路段流量
    private double speed = 0.0; //路段行程速度(米/分钟)
    private double freeSpeed; //自由流速度(米/分钟)
    private double time; //路段行程时间
    private double ticketPrice; //公共交通票价(元)

    //alpha与beta为计算旅行速度的参数
    private double alpha;
    private double beta;

    public Link() {
    }

    public Link(int id) {
        this.id = id;
    }

    public double getFreeSpeed() {
        return freeSpeed;
    }

    public void setFreeSpeed(double freeSpeed) {
        this.freeSpeed = freeSpeed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFromNode() {
        return fromNode;
    }

    public void setFromNode(int fromNode) {
        this.fromNode = fromNode;
    }

    public int getToNode() {
        return toNode;
    }

    public void setToNode(int toNode) {
        this.toNode = toNode;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
        if(this.speed > 0){
            setTime(this.length / this.speed);
        }
    }

    public TrafficMode getTrafficMode() {
        return trafficMode;
    }

    public void setTrafficMode(TrafficMode trafficMode) {
        this.trafficMode = trafficMode;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = Math.max(volume, 0);
        calculateTravelSpeed();
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        if(this.length > 0 && this.speed != 0.0){
            setTime(this.length / this.speed);
        }
    }

    public double getTime() {
        calculateTravelSpeed();
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    //计算路段的旅行速度
    public void calculateTravelSpeed(){
        if(trafficMode == TrafficMode.CAR){
            if(freeSpeed > 0 && capacity > 0){
                double travelSpeed = freeSpeed / (1 + alpha * Math.pow(volume / capacity, beta)); //计算旅行速度
                setSpeed(travelSpeed);
            }
        }else if(trafficMode == TrafficMode.METRO){
            setSpeed(MyUtils.metroSpeed);
        }else if(trafficMode == TrafficMode.BUS){
            setSpeed(MyUtils.busSpeed);
        }else{
            setSpeed(MyUtils.walkSpeed); //步行速度 m/min
        }

    }

    //路段交通饱和度
    public double getSaturation(){
        return volume / capacity;
    }

    @Override
    public String toString() {
        return "Link{" +
                "id=" + id +
                ", fromNode=" + fromNode +
                ", toNode=" + toNode +
                ", length=" + length +
                ", trafficMode=" + trafficMode +
                ", capacity=" + capacity +
                ", volume=" + volume +
                ", speed=" + speed +
                ", freeSpeed=" + freeSpeed +
                ", time=" + time +
                '}';
    }
}
