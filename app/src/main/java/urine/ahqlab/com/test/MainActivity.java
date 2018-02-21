package urine.ahqlab.com.test;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;



public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 , Serializable{

    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInput;
    private Mat matResult;
    private Mat matGray;

    private Mat sImageInput;
    private Mat sImageResult;
    private Mat sImageGray;

    private int mRoiWidth;
    private int mRoiHeight;
    private int mRoiX;
    private int mRoiY;
    private double m_dWscale;
    private double m_dHscale;

    public static final int recSize = 1000;
    public static final int smailRecSize = 60;

    private Mat area;
    private Mat areaResult;

    private RelativeLayout mainLayout;

    private Button shutter;
    private Button back;
    private Button send;
    private int shutterFlag = 0;

    public native void ConvertRGBtoGray(long matAddrInput, long matGray, long matAddrResult);
    public native void CustomActivityFindView(long matAddrInput, long matGray, long matAddrResult);
    public native void SharpenTest(long matAddrInput, long matAddrResult);

    List<MatOfPoint> matOfPoints;


    Rect rect;

    private ImageView mImageCapture;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //퍼미션 상태 확인
            if (!hasPermissions(PERMISSIONS)) {

                //퍼미션 허가 안되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
        shutter = (Button) findViewById(R.id.shutter);
        shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shutterFlag = 1;
                //Bitmap result = makeRoi(matInput, matOfPoints.get(0));

            }
        });


        back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, String.valueOf(matOfPoints.toArray().length), Toast.LENGTH_LONG).show();
              /*  Log.e("matOfPoints ? " , String.valueOf(matOfPoints.toArray().length));
                for (int i = 0; i <  matOfPoints.size(); i++) {
                    matOfPoints.remove(i);
                }
                Toast.makeText(MainActivity.this, String.valueOf(1234), Toast.LENGTH_LONG).show();*/
                matOfPoints.clear();
            }
        });
        send = (Button) findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // goToPage();
               // List<Mat> sds;
                createImageView();
                //Bitmap result = makeRoi(matInput, matOfPoints.get(0));
                /*Imgproc.rectangle(matInput, rect.tl(), rect.br(), new Scalar(255, 0, 0));
                //Mat imageRIO = new Mat();
                //matInput.submat(rect);
                //matInput.copyTo(imageRIO);
                Rect rect = Imgproc.boundingRect(matOfPoints.get(0));
                Mat mat = new Mat(matInput, rect);

                Bitmap bmp_result = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bmp_result);

                representativeColor(bmp_result);
                mImageCapture.setVisibility(View.VISIBLE);
                mImageCapture.setImageBitmap(bmp_result);*/

                //Log.e("bitmap??", String.valueOf(bmp_result.getWidth()));
                //Log.e("asdasdasd","asdasdasd");
                //Log.e("rect.width : " , String.valueOf(rect.width));
                //Log.e("rect.size : " , String.valueOf(rect.size()));

                //BitmapDrawable drawable = (BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher_background);
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.roo);
                representativeColor(icon);
                //Bitmap bitmap = drawable.getBitmap();


                //
                //mImageCapture.setImageBitmap(icon);

                //Intent intent = new Intent(MainActivity.this, DetectResultActivity.class);
                //intent.putExtra("image", bmp_result);
                //startActivity(intent);

                //ImageView imageview = new ImageView(MainActivity.this);
                //RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                //imageview.setImageBitmap(bmp_result);
                //mainLayout.addView(imageview);
            }
        });
        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                System.out.println("<<<< " + v.getHeight());
                System.out.println("<<<< " + v.getWidth());
                return false;
            }
        });
        mImageCapture = (ImageView) findViewById(R.id.image_capture);


        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {
        Vector<Vector<Point>> rects;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        matGray = inputFrame.gray();
        if ( matResult != null ) matResult.release();
        matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());

        //CustomActivityFindView(matInput.getNativeObjAddr(),  matGray.getNativeObjAddr() , matResult.getNativeObjAddr());
        //ConvertRGBtoGray(matInput.getNativeObjAddr(),  matGray.getNativeObjAddr() , matResult.getNativeObjAddr());
       // matOfPoints = (List<MatOfPoint>) SquareDetector.detectBiggestSquare(matInput);

        matOfPoints = (List<MatOfPoint>) SquareDetector.detectBiggestSquare(matInput);
        Imgproc.drawContours(matInput, matOfPoints, -1, new Scalar(0, 255, 0) ,2);

        /*if(shutterFlag == 1){

            shutterFlag = 0;
        }else if(shutterFlag == 0){
            Imgproc.drawContours(matInput, matOfPoints, -1, new Scalar(0, 255, 0) ,2);
        }
       // for (int i = 0; i <  matOfPoints.size(); i++) {
        //    makeRoi(matInput, matOfPoints.get(i));
            //setLabel(matInput, String.valueOf(matOfPoints), matOfPoints.get(i));
        //}*/

        //applyCLAHE(matInput, matInput);//Apply the CLAHE algorithm to input color image.

        return matInput;
    }
    private void goToPage(){
        long addr = matInput.getNativeObjAddr();
        Intent intent = new Intent(MainActivity.this, DetectResultActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("imageList", (Serializable) matOfPoints);
        intent.putExtras(bundle);
        intent.putExtra("image", addr);
        startActivity(intent);
    }
    private void representativeColor(Bitmap bitmap){
        int color = 0xFFFFFF; // default white
        Palette.Builder pb = Palette.from(bitmap);
        Palette palette = pb.generate();
        if (palette != null && palette.getLightVibrantSwatch() != null) {
            color = palette.getLightVibrantSwatch().getRgb();
        }else if (palette != null && palette.getDarkVibrantSwatch() != null) {
            color = palette.getDarkVibrantSwatch().getRgb();
        } else if (palette != null && palette.getDarkMutedSwatch() != null) {
            color = palette.getDarkMutedSwatch().getRgb();
        } else if (palette != null && palette.getLightMutedSwatch() != null) {
            color = palette.getLightMutedSwatch().getRgb();
        }
        Log.e(TAG, "dominantColorFromBitmap = " + Integer.toString(color, 16));
    }

    private static Bitmap makeRoi(Mat img, MatOfPoint point1){
        Rect rect = Imgproc.boundingRect(point1);
        Log.e("rect.size : " , String.valueOf(rect.size()));
        Log.e("rect.width : " , String.valueOf(rect.width));
        Mat m_matRoi = img.submat(rect);
        Bitmap bmp_result = Bitmap.createBitmap(m_matRoi.cols(), m_matRoi.rows(), Bitmap.Config.ARGB_8888);
        return bmp_result;
    }

    private static MatOfPoint approxPolyDP (MatOfPoint curve, double epsilon, boolean closed) {
        MatOfPoint2f tempMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(curve.toArray()), tempMat, epsilon, closed);
        return new MatOfPoint(tempMat.toArray());
    }

    private Mat Squaredetector(Mat matInput, Mat matResult){
        Imgproc.cvtColor(matInput, matResult, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(matResult, matResult, new Size(5, 5), 0);
        Imgproc.Canny(matResult, matResult, 75, 200);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Imgproc.findContours(matResult, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        matResult = matInput.clone();

        List<MatOfPoint> rects = new ArrayList<MatOfPoint>();

        for (int i = 0; i < contours.size(); i++) {
            double epsilon = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true) * 0.02;
            MatOfPoint approx = approxPolyDP(contours.get(i), epsilon, true);

            if (approx.toArray().length == 4 && Math.abs(Imgproc.contourArea(approx)) > 100  && Imgproc.isContourConvex(approx)) {
                double maxCosine = 0;
                for (int j = 2; j < 5; ++j) {
                    //double cosine = fabs(angle(approx[j % 4], approx[j - 2], approx[j - 1]));
                    maxCosine = Math.max(maxCosine, Math.abs(angle(approx.toArray()[j % 4], approx.toArray()[j - 2], approx.toArray()[j - 1])));
                }
                rects.add(approx);
                if (maxCosine < 1.5) {

                }
            }
        }
        Imgproc.drawContours(matResult, rects, -1, new Scalar(0, 255, 0) ,2);

        return matResult;
    }


    public static void setLabel(Mat im, String label, MatOfPoint contour)
    {
        int fontface = 3;
        double scale = 0.4;
        int thickness = 1;
        int[] baseline = {0};
        Point pt;
        Size text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline);

        //getTextSize(label, fontface, scale, thickness, baseline);
        Rect r = Imgproc.boundingRect((MatOfPoint) contour);

        pt = new Point(r.x + ((r.width - text.width) / 2), r.y + ((r.height + text.height) / 2));
        //pt1 = new Point(0, baseline[0]);
        //pt2 = new Point(text.width, -text.height);
        Imgproc.rectangle(im, pt /*Point(0, baseline)*/, pt/*Point(text.width, -text.height)*/,new Scalar(255,255,255), thickness - 2);
        //rectangle(im, pt /*Point(0, baseline)*/, pt/*Point(text.width, -text.height)*/,new Scalar(255,255,255), thickness - 2);
        //putText(im, label, pt, fontface, scale, new Scalar(0,0,0), thickness, 8);
        Imgproc.putText(im, label, pt, fontface, scale, new Scalar(0,0,0), thickness);
    }
    //쉽고 재미있는, 코칭, 파트너,
    private Mat Squaredetector2(Mat matInput, Mat matResult){
        Imgproc.cvtColor(matInput, matResult, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(matResult, matResult, new Size(5, 5), 0);
        Imgproc.Canny(matResult, matResult, 75, 200);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Imgproc.findContours(matResult, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        matResult = matInput.clone();

        List<MatOfPoint> rects = new ArrayList<MatOfPoint>();

        //for (int i = 0; i < contours.size(); i++) {
        for (int i = 0; i <  contours.size(); i++) {
            Log.e("<<<<<<<<<<<<<","<<<<<<<<");
            double epsilon = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true) * 0.02;
            MatOfPoint approx = approxPolyDP(contours.get(i), epsilon, true);

            if (approx.toArray().length == 4 && Math.abs(Imgproc.contourArea(approx)) > 1000  && Imgproc.isContourConvex(approx)) {
                double maxCosine = 0;
                for (int j = 2; j < 5; ++j) {
                    maxCosine = Math.max(maxCosine, Math.abs(angle(approx.toArray()[j % 4], approx.toArray()[j - 2], approx.toArray()[j - 1])));
                }
                if (maxCosine < 0.3) {
                    rects.add(approx);
                }
            }
        }
        shutterFlag = 0;
        Imgproc.drawContours(matResult, rects, -1, new Scalar(0, 255, 0) ,2);
        return matResult;
    }

    private static double angle (Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;

        return (dx1 * dx2 + dy1 * dy2) /
                Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    private void drawRectangle(Rect rect){
        Log.e("rect.x", String.valueOf(rect.x));
        Log.e("rect.x", String.valueOf(rect.y));
        Log.e("rect.x+rect.width", String.valueOf(rect.x+rect.width));
        Log.e("rect.y+rect.height", String.valueOf(rect.y+rect.height));
        Imgproc.rectangle(matInput, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0, 255, 0));
    }
    //여기서부턴 퍼미션 관련 메소드
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS  = {"android.permission.CAMERA"};


    private boolean hasPermissions(String[] permissions) {
        int result;

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){

            result = ContextCompat.checkSelfPermission(this, perms);

            if (result == PackageManager.PERMISSION_DENIED){
                //허가 안된 퍼미션 발견
                return false;
            }
        }
        //모든 퍼미션이 허가되었음
        return true;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){

            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted)
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                }
                break;
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }

    private void createImageView(){
        ImageView imageView = new ImageView(MainActivity.this);
        imageView.setBackgroundColor(Color.BLUE);
        LinearLayout.LayoutParams params =  new LinearLayout.LayoutParams(13, 13);
        params.setMargins(3,3,3,3);
        imageView.setLayoutParams(params);
        LinearLayout myLayout = (LinearLayout) findViewById(R.id.colorChart);
        myLayout.addView(imageView);
    }


    public void applyCLAHE(Mat srcArry, Mat dstArry) {
        //Function that applies the CLAHE algorithm to "dstArry".

        if (srcArry.channels() >= 3) {
            // READ RGB color image and convert it to Lab
            Mat channel = new Mat();
            Imgproc.cvtColor(srcArry, dstArry, Imgproc.COLOR_BGR2Lab);

            // Extract the L channel
            Core.extractChannel(dstArry, channel, 0);

            // apply the CLAHE algorithm to the L channel
            CLAHE clahe = Imgproc.createCLAHE();
            clahe.setClipLimit(4);
            clahe.apply(channel, channel);

            // Merge the the color planes back into an Lab image
            Core.insertChannel(channel, dstArry, 0);

            // convert back to RGB
            Imgproc.cvtColor(dstArry, dstArry, Imgproc.COLOR_Lab2BGR);

            // Temporary Mat not reused, so release from memory.
            channel.release();
        }

    }
}