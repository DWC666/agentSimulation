import core.SimulationMain;
import org.jfree.data.xy.XYDataset;
import util.ChartUtil;
import util.ExcelUtil;

import java.io.IOException;

public class Plot {
    //绘制各停车场的泊位占有率和排队人数的图像
    public static void p1() throws IOException {
        SimulationMain.simulate(0);
        ExcelUtil.writeDataToExcel(SimulationMain.totalBerthOccupy, "变动1.8-占有率");
        ExcelUtil.writeDataToExcel(SimulationMain.parkFee, "变动1.8-停车费");
        XYDataset xyDataset = ChartUtil.createXYDataset("停车场", SimulationMain.totalBerthOccupy, 1);
        ChartUtil.plotXYLineChart("占有率", xyDataset, "时间(分钟)", "占有率", SimulationMain.iterationNum);
    }

    public static void main(String[] args) throws IOException {
        p1();
    }
}
