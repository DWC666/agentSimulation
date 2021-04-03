import core.SimulationMain;
import org.jfree.data.xy.XYDataset;
import util.ChartUtil;
import util.ExcelUtil;

import java.io.IOException;

public class Plot {
    //绘制各停车场的泊位占有率和排队人数的图像
    public static void p1() throws IOException {
        SimulationMain.simulate(0);
        XYDataset xyDataset = ChartUtil.createXYDataset("停车场", SimulationMain.totalBerthOccupy, 1);
        ExcelUtil.writeDataToExcel(SimulationMain.queueNumber, "排队人数");
        ExcelUtil.writeDataToExcel(SimulationMain.totalBerthOccupy, "占有率");
        ExcelUtil.writeDataToExcel(SimulationMain.parkFee, "停车费");
        ChartUtil.plotXYLineChart("停车场排队人数", xyDataset, "时间(分钟)", "排队人数", SimulationMain.iterationNum);
    }

    public static void main(String[] args) throws IOException {
        p1();
    }
}
