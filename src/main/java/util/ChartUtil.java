package util;

import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.xml.crypto.Data;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class ChartUtil {
    //保存图像的大小
    public static final int width = 800;
    public static final int height = 600;

    public static void plotLineChart(String titleName, CategoryDataset dataset, String xAxisName, String yAxisName) {
        // 创建主题样式
        StandardChartTheme standardChartTheme = new StandardChartTheme("CN");
        // 设置标题字体
        standardChartTheme.setExtraLargeFont(new Font("黑体", Font.BOLD, 20));
        // 设置图例的字体
        standardChartTheme.setRegularFont(new Font("黑体", Font.PLAIN, 15));
        // 设置轴向的字体
        standardChartTheme.setLargeFont(new Font("黑体", Font.PLAIN, 15));
        // 应用主题样式
        ChartFactory.setChartTheme(standardChartTheme);

        JFreeChart lineChart = ChartFactory.createLineChart(titleName, xAxisName, yAxisName, dataset);
//        ChartFactory.createXYLineChart(titleName, xAxisName, yAxisName, dataset);

        XYPlot plot = lineChart.getXYPlot();
        CategoryPlot categoryPlot = lineChart.getCategoryPlot();
        ValueAxis domainAxis1 = plot.getDomainAxis();
        NumberAxis domainAxis = new NumberAxis(); //横坐标
        CategoryAxis categoryAxis = new CategoryAxis();
        domainAxis.setTickLabelFont(new Font("宋书", Font.PLAIN, 20));
        domainAxis.setRange(1, 1440); //坐标范围
        domainAxis.setTickUnit(new NumberTickUnit(10));


        //创建窗体
        ChartFrame chartFrame = new ChartFrame("饼状图", lineChart);
        chartFrame.setSize(400, 400);
        chartFrame.setLocationRelativeTo(null);
        chartFrame.setVisible(true);
    }


    public static void plotXYLineChart(String titleName, XYDataset dataset, String xAxisName, String yAxisName, int dataSize) throws IOException {
        // 创建主题样式
        StandardChartTheme standardChartTheme = new StandardChartTheme("CN");
        // 设置标题字体
        standardChartTheme.setExtraLargeFont(new Font("黑体", Font.BOLD, 40));
        // 设置图例的字体
        standardChartTheme.setRegularFont(new Font("黑体", Font.BOLD, 30));

        // 设置轴向的字体
//        standardChartTheme.setLargeFont(new Font("黑体", Font.PLAIN, 15));
        // 应用主题样式
        ChartFactory.setChartTheme(standardChartTheme);


        JFreeChart chart = ChartFactory.createXYLineChart(titleName, xAxisName, yAxisName, dataset);
        //保存为图片
//        ChartUtils.saveChartAsJPEG(new File("E:\\毕业\\pic.jpeg"), chart, width, height);
        //设置图例
//        LegendTitle legend = chart.getLegend();
//        legend.setWidth(10);
//        legend.setPosition(RectangleEdge.TOP);
//        legend.setPadding(new RectangleInsets());

        XYPlot plot = chart.getXYPlot();

        // 设置样式
        XYItemRenderer renderer = plot.getRenderer();

        //设置线条宽度
        renderer.setDefaultStroke(new BasicStroke(40.0f));
        //设置线条颜色
        renderer.setSeriesPaint(0, Color.red);
        renderer.setSeriesPaint(1, Color.green);
        renderer.setSeriesPaint(2, Color.orange);
        renderer.setSeriesPaint(3, Color.blue);
        renderer.setSeriesPaint(4, Color.CYAN);
        renderer.setSeriesPaint(5, Color.magenta);

        //设置背景色
        plot.setBackgroundPaint(Color.white);
        //设置横向网格线为黑色
        plot.setRangeGridlinePaint(Color.BLACK);
        //设置竖向网格线为黑色
        plot.setDomainGridlinePaint(Color.BLACK);
        //横坐标
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        //设置X轴坐标上的文字
        domainAxis.setTickLabelFont(new Font("黑体", Font.PLAIN, 25));
        //设置X轴的标题文字
        domainAxis.setLabelFont(new Font("黑体", Font.BOLD, 30));
        //刻度范围
        domainAxis.setRange(0, dataSize + 1);
        //刻度间隔
        domainAxis.setTickUnit(new NumberTickUnit(120));

        //纵坐标
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0, 1);
        rangeAxis.setTickUnit(new NumberTickUnit(0.1));
        rangeAxis.setTickLabelFont(new Font("黑体", Font.PLAIN, 25));
        rangeAxis.setLabelFont(new Font("黑体", Font.BOLD, 30));

        //设置右侧y轴
//        NumberAxis numberAxis = new NumberAxis("y2");
//        numberAxis.setRange(1,2);
//        plot.setRangeAxis(1, numberAxis);
//        plot.setDataset(1, dataset2);
//        plot.mapDatasetToRangeAxis(1, 1);


        //创建窗体
        ChartFrame chartFrame = new ChartFrame("折线图", chart);
        chartFrame.setSize(width, height);
        chartFrame.setLocationRelativeTo(null);
        chartFrame.setVisible(true);
    }


    public static void plotXYLineChart2(String titleName, XYDataset dataset, String xAxisName, String yAxisName) throws IOException {
        // 创建主题样式
        StandardChartTheme standardChartTheme = new StandardChartTheme("CN");
        // 设置标题字体
        standardChartTheme.setExtraLargeFont(new Font("黑体", Font.BOLD, 40));
        // 设置图例的字体
        standardChartTheme.setRegularFont(new Font("黑体", Font.BOLD, 30));

        // 设置轴向的字体
//        standardChartTheme.setLargeFont(new Font("黑体", Font.PLAIN, 15));
        // 应用主题样式
        ChartFactory.setChartTheme(standardChartTheme);


        JFreeChart chart = ChartFactory.createXYLineChart(titleName, xAxisName, yAxisName, dataset);

        XYPlot plot = chart.getXYPlot();

        // 设置样式
        XYItemRenderer renderer = plot.getRenderer();

        //设置线条宽度
        renderer.setDefaultStroke(new BasicStroke(40.0f));


        //设置背景色
        plot.setBackgroundPaint(Color.white);
        //设置横向网格线为黑色
        plot.setRangeGridlinePaint(Color.BLACK);
        //设置竖向网格线为黑色
        plot.setDomainGridlinePaint(Color.BLACK);
        //横坐标
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        //设置X轴坐标上的文字
        domainAxis.setTickLabelFont(new Font("黑体", Font.PLAIN, 10));
        //设置X轴的标题文字
        domainAxis.setLabelFont(new Font("黑体", Font.BOLD, 30));
        //刻度范围
//        domainAxis.setRange(0,  1);
        //刻度间隔
        domainAxis.setTickUnit(new NumberTickUnit(1));

        //纵坐标
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(200, 1800);
        rangeAxis.setTickUnit(new NumberTickUnit(1));
        rangeAxis.setTickLabelFont(new Font("黑体", Font.PLAIN, 25));
        rangeAxis.setLabelFont(new Font("黑体", Font.BOLD, 30));


        //创建窗体
        ChartFrame chartFrame = new ChartFrame("折线图", chart);
        chartFrame.setSize(width, height);
        chartFrame.setLocationRelativeTo(null);
        chartFrame.setVisible(true);
    }


    public static CategoryDataset createCategoryDataset(double[] data, String seriesName, String[] xAxis) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < data.length; i++) {
            if (xAxis == null) {
                dataset.addValue(data[i], seriesName, String.valueOf(i));
            } else {
                dataset.addValue(data[i], seriesName, xAxis[i]);
            }
        }
        return dataset;
    }


    /**
     *
     * @param seriesName 数据标签
     * @param xData x轴数据
     * @param yData y轴数据
     * @return
     */
    public static XYDataset createXYDataset( String seriesName, double[] xData, double[] yData) {
        XYSeries xySeries = new XYSeries(seriesName);
        if(xData == null){
            for(int i=0; i<yData.length; i++){
                xySeries.add(i+1, yData[i]);

            }
        }else{
            for(int i=0; i<yData.length; i++){
                xySeries.add(xData[i], yData[i]);
            }
        }
        return new XYSeriesCollection(xySeries);
    }


    public static XYDataset createXYDataset(String baseSeriesName, double[][] dataSet, int step){
        int n = dataSet.length;
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        for(int i=0; i<n; i++){
            XYSeries xySeries = new XYSeries(baseSeriesName + i);
            for(int j=0; j<dataSet[i].length; j++){
                if(j % step == 0){
                    xySeries.add(j+1, dataSet[i][j]);
                }
            }
            xySeriesCollection.addSeries(xySeries);
        }
        return xySeriesCollection;
    }




    public static double[] fun() {
        double[] data = new double[1440];
        for (int i = 1; i <= 1440; i++) {
            double v;
            if(i <= 227){
                v =  0.00212 * i + 0.321;
            }else if(i <= 1145){
                v = 0.2 * Math.sin(i * Math.PI / 300 - 2.32) + 0.8;
            }else{
                v = -0.00171 * i + 2.7048;
            }
            data[i - 1] = v;
        }
        return data;
    }

    public static double[] fun3() {
        double[] data = new double[100];
        for (int i = 1; i <= data.length; i++) {
            double v;
            if(i <= 227){
                v =  (i + 60) * 0.04 / 60;
            }else if(i <= 1145){
                v = 0.72 * Math.sin(i * Math.PI / 750 - Math.PI * 360 / 750) + 0.28;
            }else{
                v = 0.0005 * (1080 - i) + 0.37;
            }
            System.out.println(i + " --- " + v);
            data[i - 1] = v;
        }
        return data;
    }

    public static double[] sin(double a, double b, double c, double d) {
        double[] data = new double[30];
        double x = Math.PI * (-1);
        for (int i = 0; i < data.length; i++) {
            data[i] = a * Math.sin(b * x + c) + d;
            x += Math.PI / 8;
//            data[i] = Math.sin(i);
        }
        return data;
    }



    public static void main(String[] args) throws IOException {

        double[] data1 = new double[]{0.11416241361251168, 0.1125256615055742, 0.11198183894929974, 0.11217080087583246, 0.11195155302276787, 0.11215944895050001, 0.11194855113449785, 0.11181280531683785, 0.11194245463042932, 0.1116599682214499};
        double[] data2 = new double[]{1,1,1,1,1,1,1,1,1,1};
        XYSeries xySeries1 = new XYSeries("1");
        for(int i=0; i<data1.length; i++){
            xySeries1.add(i+1, data1[i]);
        }
        XYSeries xySeries2 = new XYSeries("2");
        for(int i=0; i<data2.length; i++){
            xySeries2.add(i+1, data2[i]);
        }
        XYSeriesCollection xySeriesCollection1 = new XYSeriesCollection();
        xySeriesCollection1.addSeries(xySeries1);
        plotXYLineChart("折线图", xySeriesCollection1, "时间", "值", data1.length);

    }
}

