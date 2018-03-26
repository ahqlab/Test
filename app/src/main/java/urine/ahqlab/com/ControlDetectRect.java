package urine.ahqlab.com;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

import urine.ahqlab.com.test.R;
import urine.ahqlab.com.test.SquareDetector;

public class ControlDetectRect extends AppCompatActivity {

    private Button left;
    private Button right;
    private Button confirm;

    private ImageView imageVIewInput;
    private TextView total;
    private TextView currentIndex;
    private ImageView transformView;
    private LinearLayout averigeColorSquares;
    private LinearLayout averigeColorRgbs;

    private String filename;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }


    private static final String TAG = "opencv";
    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS  = {"android.permission.WRITE_EXTERNAL_STORAGE"};


    private Mat img_input;
    private Mat img_output;
    private Mat img_gray;

    List<MatOfPoint> matOfPoints;

    public native void loadImage(String imageFileName, long img);
    public native void imageprocessing(long inputImage, long outputImage, long outputImageGray);
    //public native void ImageSharpening(long matAddrInput, long matGray, long matAddrResult);

    int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_detect_rect);
        Intent intent = getIntent();
        filename = intent.getExtras().getString("filename");
        imageVIewInput = (ImageView) findViewById(R.id.imageViewOutput);
        left = (Button) findViewById(R.id.left);
        right = (Button) findViewById(R.id.right);
        confirm = (Button) findViewById(R.id.confirm);
        total = (TextView) findViewById(R.id.total);
        currentIndex = (TextView) findViewById(R.id.currentIndex);
        transformView = (ImageView) findViewById(R.id.transformView);
        averigeColorSquares = (LinearLayout) findViewById(R.id.averigeColorSquares);
        averigeColorRgbs = (LinearLayout) findViewById(R.id.averigeColorRgbs);
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawRectForIndexPlus(img_gray);
            }
        });
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawRectForIndexMinus(img_gray);
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //PerspectiveTransform 을 수행한다.
                MatOfPoint sourceMat  = matOfPoints.get(i);
                List<Point> lp = sourceMat.toList();
                Point rectA;
                Point rectB;
                Point rectC;
                Point rectD;
                if(lp.get(0).x > lp.get(1).x){
                    rectD = lp.get(0);
                    rectA = lp.get(1);
                    rectB = lp.get(2);
                    rectC = lp.get(3);
                }else {
                    rectA = lp.get(0);
                    rectB = lp.get(1);
                    rectC = lp.get(2);
                    rectD = lp.get(3);
                }

                Log.e("HJLEE", "rectA.x : " + rectA.x + " rectA.y : " + rectA.y);
                Log.e("HJLEE", "rectB.x : " + rectB.x + " rectB.y : " + rectB.y);
                Log.e("HJLEE", "rectC.x : " + rectC.x + " rectC.y : " + rectC.y);
                Log.e("HJLEE", "rectD.x : " + rectD.x + " rectD.y : " + rectD.y);

                Rect rect = Imgproc.boundingRect(matOfPoints.get(i));
                List<Point> src_pnt = new ArrayList<Point>();
                src_pnt.add(rectA);
                src_pnt.add(rectB);
                src_pnt.add(rectC);
                src_pnt.add(rectD);
                Mat startM = Converters.vector_Point2f_to_Mat(src_pnt);

                List<Point> dst_pnt = new ArrayList<Point>();
                Point p4 = new Point(10.0, 10.0);
                dst_pnt.add(p4);
                Point p5 = new Point(10.0, rect.height);
                dst_pnt.add(p5);
                Point p6 = new Point(rect.width, rect.height);
                dst_pnt.add(p6);
                Point p7 = new Point(rect.width, 10.0);
                dst_pnt.add(p7);
                Mat endM = Converters.vector_Point2f_to_Mat(dst_pnt);

                Mat M = Imgproc.getPerspectiveTransform(startM, endM);
                Imgproc.warpPerspective(img_gray, img_output, M, new Size(rect.width, rect.height));
                Size sz = new Size(1600,123);
                Imgproc.resize( img_output, img_output, sz);
                Mat newPTImage = img_output.clone();

                setDrawRect(img_output, newPTImage);

                setLabel(img_input, "0", rectA);
                setLabel(img_input, "1", rectB);
                setLabel(img_input, "2", rectC);
                setLabel(img_input, "3", rectD);

                Bitmap bitmapOutput = Bitmap.createBitmap(img_output.cols(), img_output.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img_output, bitmapOutput);
                transformView.setImageBitmap(bitmapOutput);
            }
        });

        if (!hasPermissions(PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            requestNecessaryPermissions(PERMISSIONS);//퍼미션 허가안되어 있다면 사용자에게 요청
        } else {
            //이미 사용자에게 퍼미션 허가를 받음.
            read_image_file(filename);
            imageprocess_and_showResult();
        }

    }

    private void setDrawRect(Mat img, Mat img2){

        List<Rect> rects = new ArrayList<Rect>();

        double rectTopPointY = 25;
        double rectBottomPointY = 95;
        double rectWidth = 60;
        double interval = 138;

        //Imgproc.rectangle(img, new Point(10 , rectTopPointY), new Point( 10 + rectWidth, rectBottomPointY),   new Scalar(0, 255, 0), 2);
        //rects.add(new Rect(new Point(10 , rectTopPointY), new Point( 10 + rectWidth, rectBottomPointY)));
        for(int i = 0; i < 11; i++){
            Imgproc.rectangle(img, new Point(60 + (interval * i), rectTopPointY), new Point( 60 + (interval * i) + rectWidth, rectBottomPointY),   new Scalar(0, 255, 0), 2);
            rects.add(new Rect(new Point(60 + (interval * i), rectTopPointY), new Point( 60 + (interval * i) + rectWidth, rectBottomPointY)));
        }
        makeSquare(img, rects);
        makeRGBCode(img, rects);
    }

    private void makeRGBCode(Mat img, List<Rect> rects) {
        for (int i = 0; i < rects.size(); i++) {
            Mat mat = new Mat(img, rects.get(i));
            //평균 RGB색을 구한다.
            List<Integer> rgbs = getPixcel(mat);
            makeAverigeRGBCode(mat, rgbs);
        }
    }

    public void makeAverigeRGBCode(Mat mat, List<Integer> rgbs){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(120, 120);
        lp.setMargins(5,5,5,5);
        TextView textView = new TextView(ControlDetectRect.this);
        textView.setTextSize(8);
        TextView textView2 = new TextView(ControlDetectRect.this);
        String r = Integer.toHexString(rgbs.get(0));
        String g = Integer.toHexString(rgbs.get(1));
        String b = Integer.toHexString(rgbs.get(2));
        textView.setText(String.valueOf(r+g+b));
        textView2.setText(", ");
        averigeColorRgbs.addView(textView);
        averigeColorRgbs.addView(textView2);
    }

    private List<Integer> getPixcel(Mat image){
        List<Integer> colors = new ArrayList<Integer>();
        List<Integer> rTemp = new ArrayList<Integer>();
        List<Integer> gTemp = new ArrayList<Integer>();
        List<Integer> bTemp = new ArrayList<Integer>();
        for (int j = 1; j < image.rows() - 1; ++j) {
            for (int i = 1; i < image.cols() - 1; ++i) {
                int red = (int) image.get(j,i)[0];
                int green = (int) image.get(j,i)[1];
                int blue = (int) image.get(j,i)[2];
                rTemp.add(red);
                gTemp.add(green);
                bTemp.add(blue);
            }
        }
        //System.out.println("R : " + getMean(rTemp) + " G : " + getMean(gTemp) + " B : " + getMean(bTemp));
        colors.add(getMean(rTemp));
        colors.add(getMean(gTemp));
        colors.add(getMean(bTemp));
        return colors;
    }

    public int getMean(List<Integer> array) {
        int sum = 0;
        int average = 0;
        for (int i = 0; i < array.size(); i++) {
            sum += array.get(i);
        }
        average = sum/array.size();
        return average;
    }

    private void  makeSquare(Mat img, List<Rect> rects) {
        for (int i = 0; i < rects.size(); i++) {
            Mat mat = new Mat(img, rects.get(i));
            createImageView(mat, i);
        }
    }

    public void createImageView(Mat mat, int num) {

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(60, 60);
        lp.setMargins(5, 5, 5, 5);
        ImageView imageView = new ImageView(ControlDetectRect.this);
        imageView.setLayoutParams(lp);
        Bitmap bitmapInput = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmapInput);
        imageView.setImageBitmap(bitmapInput);
        averigeColorSquares.addView(imageView);
    }
    public static void setLabel(Mat im, String label, Point pt)
    {
        int fontface = 3;
        double scale = 5;
        int thickness = 1;
        int[] baseline = {0};
        //Point pt;
        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);

        //getTextSize(label, fontface, scale, thickness, baseline);
        //Rect r = Imgproc.boundingRect((MatOfPoint) contour);

        //pt = new Point(r.x + ((r.width - text.width) / 2), r.y + ((r.height + text.height) / 2));
        //pt1 = new Point(0, baseline[0]);
        //pt2 = new Point(text.width, -text.height);
        Imgproc.rectangle(im, pt /*Point(0, baseline)*/, pt/*Point(text.width, -text.height)*/,new Scalar(255,0,0), thickness - 2);
        //rectangle(im, pt /*Point(0, baseline)*/, pt/*Point(text.width, -text.height)*/,new Scalar(255,255,255), thickness - 2);
        //putText(im, label, pt, fontface, scale, new Scalar(0,0,0), thickness, 8);
        Imgproc.putText(im, label, pt, fontface, scale, new Scalar(255,0,0), thickness);
    }

    @SuppressLint("WrongConstant")
    private boolean hasPermissions(String[] permissions) {
        int ret = 0;
        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){
            ret = checkCallingOrSelfPermission(perms);
            if (!(ret == PackageManager.PERMISSION_GRANTED)){
                //퍼미션 허가 안된 경우
                return false;
            }
        }
        //모든 퍼미션이 허가된 경우
        return true;
    }

    private void requestNecessaryPermissions(String[] permissions) {
        //마시멜로( API 23 )이상에서 런타임 퍼미션(Runtime Permission) 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void read_image_file(String filename) {
        //copyFile(filename);

        img_input = new Mat();
        img_output = new Mat();
        img_gray = new Mat();

        loadImage(filename, img_input.getNativeObjAddr());
        img_gray = img_input.clone();
        //ImageSharpening(img_input.getNativeObjAddr(), img_gray.getNativeObjAddr(), img_output.getNativeObjAddr());

    }

    private void imageprocess_and_showResult() {
        imageprocessing(img_input.getNativeObjAddr(), img_output.getNativeObjAddr(), img_gray.getNativeObjAddr());

        matOfPoints = (List<MatOfPoint>) SquareDetector.detectAllSquare(img_input);
        total.setText(String.valueOf(matOfPoints.size()));
        List<MatOfPoint> firstPoint = new ArrayList<MatOfPoint>();
        firstPoint.add(matOfPoints.get(0));
        Imgproc.drawContours(img_input, firstPoint, -1, new Scalar(0, 255, 0) ,2);

        Bitmap bitmapOutput = Bitmap.createBitmap(img_input.cols(), img_input.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_input, bitmapOutput);
        imageVIewInput.setImageBitmap(bitmapOutput);
    }

    private void drawRectForIndexPlus(Mat img_input){
        new drawRectForIndexPlusTask().execute();
    }

    private class drawRectForIndexPlusTask extends AsyncTask<Void, Void, Bitmap> {
        ProgressDialog asyncDialog = new ProgressDialog(ControlDetectRect.this);
        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("검출된 Contour 를 찾고있습니다. ");
            asyncDialog.setCanceledOnTouchOutside(false);
            asyncDialog.show();
            super.onPreExecute();
        }
        @Override
        protected Bitmap doInBackground(Void... arg0) {

            Bitmap bitmapOutput;
            if(i == (matOfPoints.size() - 1)){
                Toast.makeText(getApplicationContext() ," 마지막 ", Toast.LENGTH_SHORT).show();
                Mat img = new Mat();
                loadImage(filename, img.getNativeObjAddr());
                Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
                List<MatOfPoint> newList = new ArrayList<MatOfPoint>();
                newList.add(matOfPoints.get(i));
                Imgproc.drawContours(img, newList, -1, new Scalar(0, 255, 0) ,10);

                List<Point> lp = matOfPoints.get(i).toList();
                setLabel(img_input, "0", lp.get(0));
                setLabel(img_input, "1", lp.get(1));
                setLabel(img_input, "2", lp.get(2));
                setLabel(img_input, "3", lp.get(3));

                bitmapOutput = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img, bitmapOutput);
            }else {
                i++;
                Mat img = new Mat();
                loadImage(filename, img.getNativeObjAddr());
                Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
                List<MatOfPoint> newList = new ArrayList<MatOfPoint>();
                newList.add(matOfPoints.get(i));
                Imgproc.drawContours(img, newList, -1, new Scalar(0, 255, 0), 10);
                List<Point> lp = matOfPoints.get(i).toList();
                setLabel(img_input, "0", lp.get(0));
                setLabel(img_input, "1", lp.get(1));
                setLabel(img_input, "2", lp.get(2));
                setLabel(img_input, "3", lp.get(3));
                bitmapOutput = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img, bitmapOutput);

            }
            return bitmapOutput;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            currentIndex.setText(String.valueOf(i));
            imageVIewInput.setImageBitmap(result);
            asyncDialog.dismiss();
            super.onPostExecute(result);
        }
    }

    private class drawRectForIndexMinusTask extends AsyncTask<Void, Void, Bitmap> {

        ProgressDialog asyncDialog = new ProgressDialog(ControlDetectRect.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("검출된 Contour 를 찾고있습니다.");
            asyncDialog.setCanceledOnTouchOutside(false);
            asyncDialog.show();
            super.onPreExecute();
        }
        @Override
        protected Bitmap doInBackground(Void... arg0) {
            Bitmap bitmapOutput;
            if(i == 0){
                // total.setText(String.valueOf(i));
                Toast.makeText(getApplicationContext() ," 처음 ", Toast.LENGTH_SHORT).show();
                Mat img = new Mat();
                loadImage(filename, img.getNativeObjAddr());
                Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
                List<MatOfPoint> newList = new ArrayList<MatOfPoint>();
                newList.add(matOfPoints.get(i));
                Imgproc.drawContours(img, newList, -1, new Scalar(0, 255, 0) ,10);

                bitmapOutput = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img, bitmapOutput);

            }else{
                i--;
                Mat img = new Mat();
                loadImage(filename, img.getNativeObjAddr());
                Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);
                List<MatOfPoint> newList = new ArrayList<MatOfPoint>();
                newList.add(matOfPoints.get(i));
                Imgproc.drawContours(img, newList, -1, new Scalar(0, 255, 0) ,10);

                bitmapOutput = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img, bitmapOutput);
            }
            return bitmapOutput;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            currentIndex.setText(String.valueOf(i));
            imageVIewInput.setImageBitmap(result);
            asyncDialog.dismiss();
            super.onPostExecute(result);
        }
    }

    private void drawRectForIndexMinus(Mat img_input){
        new drawRectForIndexMinusTask().execute();
    }
}
