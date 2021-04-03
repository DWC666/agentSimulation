package util;

import core.SimulationMain;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 操作Excel的工具类，使用jxl方式读取，可能只能支持xls格式的文件，对于xlsx格式就不再支持
 */
public class ExcelUtil {
    //不能设置为Integer.MAX_VALUE，否则两个Integer.MAX_VALUE相加会溢出导致出现负权
    public static final int INT_MAX = 999999;
    public static final double D_MAX = 999999.0;

    public static class MatrixResult{
        public double[][] lengthMatrix = null;
        public int[][] idMatrix = null;
        public int[][] typeMatrix = null;

        public MatrixResult(){}

        public MatrixResult(double[][] lengthMatrix, int[][] idMatrix, int[][] typeMatrix) {
            this.lengthMatrix = lengthMatrix;
            this.idMatrix = idMatrix;
            this.typeMatrix = typeMatrix;
        }
    }


    /**
     * 返回Excel文件的sheet表
     * @return
     */
    private static Sheet getSheet(){
        Sheet sheet = null;
        try{
            InputStream is = new FileInputStream("E:\\毕业\\map3.xls");
            Workbook wb = Workbook.getWorkbook(is); //对应excel文件
            sheet = wb.getSheet(0);
        }catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e){
            e.printStackTrace();
        }
        return sheet;
    }


    /**
     * 获取路网的长度矩阵、id矩阵和类型矩阵
     * @return
     */
    public static MatrixResult getMapMatrix(){
        Sheet sheet = getSheet();
        int rows = sheet.getRows();
        int linkNum = rows - 1; //第一行为列名
        Cell[] column = sheet.getColumn(0);
        Set<Integer> nodes = new HashSet<>();
        for(int i=1; i<rows; i++){
            nodes.add(Integer.valueOf(column[i].getContents()));
        }
        int nodeNum = nodes.size();

        double[][] lengthMatrix = new double[nodeNum][nodeNum];
        int[][] idMatrix = new int[nodeNum][nodeNum];
        //路段类型数组：0表示步行，1表示汽车，2表示公交，3表示地铁， 4为汽车转步行，5为步行转汽车，6为公交转步行，7为步行转公交，8为地铁转步行，9为步行转地铁
        int[][] typeMatrix = new int[nodeNum][nodeNum];

        //初始化矩阵
        for(int i=0; i<nodeNum; i++){
            Arrays.fill(lengthMatrix[i], D_MAX);
            Arrays.fill(idMatrix[i], -1); //-1则表示两节点间不存在路段
            Arrays.fill(typeMatrix[i], -1); //-1则表示两节点间不存在路段
        }

        for(int i=1; i<rows; i++){
            Cell[] row = sheet.getRow(i);
            int startNode = Integer.valueOf(row[0].getContents());
            int endNode = Integer.valueOf(row[1].getContents());
            int linkId = Integer.valueOf(row[2].getContents());
            double linkLen= Double.valueOf(row[3].getContents());
            int linkType = Integer.valueOf(row[4].getContents());

            idMatrix[startNode][endNode] = linkId;
            lengthMatrix[startNode][endNode] = linkLen;
            typeMatrix[startNode][endNode] = linkType;
        }

        return new MatrixResult(lengthMatrix, idMatrix, typeMatrix);
    }


    /**
     *将数据写入Excel
     */
    public static void writeDataToExcel(double[] data) throws IOException, WriteException {
        WritableWorkbook wb = Workbook.createWorkbook(new File("E:\\毕业\\" +"OD调节函数.xls"));
        WritableSheet sheet = wb.createSheet("sheet1", 0);
        int len = data.length;
        for(int i=0; i<len; i++){
            Label label = new Label(0, i, String.valueOf(data[i])); //第一个数表示列，第二个数表示行
            sheet.addCell(label);
        }
        wb.write();
        wb.close();
        return;
    }

    public static void writeDataToExcel(double[][] data, String fileName){
        WritableWorkbook wb = null;
        try{
            wb = Workbook.createWorkbook(new File("E:\\毕业\\" + fileName + ".xls"));
            WritableSheet sheet = wb.createSheet("sheet1", 0);
            int rows = data.length;
            int cols = data[0].length;
            for(int i=0; i<rows; i++){
                for(int j=0; j<cols; j++){
                    Label label = new Label(i, j, String.valueOf(data[i][j])); //第一个数表示列，第二个数表示行
                    sheet.addCell(label);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                wb.write();
                wb.close();
            } catch (IOException e) {
                e.printStackTrace();
            }catch (WriteException e) {
                e.printStackTrace();
            }
        }
        return;
    }



    public static void main(String[] args) throws IOException, WriteException {
//        MatrixResult matrixResult = getMapMatrix();
//        MyUtils.printMatrix(matrixResult.idMatrix);
        writeDataToExcel(new double[][]{{0.1, 0.2}, {0.3, 0.5}}, "t");
    }
}
