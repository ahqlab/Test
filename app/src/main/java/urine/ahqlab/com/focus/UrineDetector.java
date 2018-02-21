package urine.ahqlab.com.focus;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import urine.ahqlab.com.result.DetailResult;
import urine.ahqlab.com.test.R;
import urine.ahqlab.com.test.SquareDetector;

public class UrineDetector extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    public LinearLayout mainLayout;

    public LinearLayout detectingResultLayout;

    ImageView imageVIewInput;

    ImageView imageVIewOuput;

    private Mat img_input;
    private Mat img_output;
    private Mat img_gray;

    public Button getRect;

    private String filename;

    List<MatOfPoint> matOfPoints;

    List<MatOfPoint> squeres;

    public int whiteRed;
    public int whiteGreen;
    public int whiteBlue;

    private static final String TAG = "opencv";
    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS  = {"android.permission.WRITE_EXTERNAL_STORAGE"};

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

    private void copyFile(String filename) {
        String baseDir = Environment.getExternalStorageDirectory().getPath();
        String pathDir = baseDir + File.separator + filename;

        AssetManager assetManager = this.getAssets();

        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);
            inputStream = assetManager.open(filename);
            outputStream = new FileOutputStream(pathDir);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString());
        }
    }

    public void createImageView(){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(5,5,5,5);
        lp.gravity= Gravity.RIGHT;
        lp.setMargins(20, 20, 20, 20);
        Button button = new Button(UrineDetector.this);
        button.setText("결과보기");
        button.setTextSize(40);
        mainLayout.addView(button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UrineDetector.this, DetailResult.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){

            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if (!writeAccepted )
                        {
                            showDialogforPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                            return;
                        }else
                        {
                            read_image_file(filename);
                            imageprocess_and_showResult();
                        }
                    }
                }
                break;
        }
    }

    private void showDialogforPermission(String msg) {

        final AlertDialog.Builder myDialog = new AlertDialog.Builder(  UrineDetector.this);
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }

            }
        });
        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urine_detector);

        Intent intent = getIntent();
        filename = intent.getExtras().getString("filename");

        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);

        imageVIewInput = (ImageView) findViewById(R.id.imageViewInput);

        imageVIewOuput = (ImageView) findViewById(R.id.imageViewOutput);

        detectingResultLayout = (LinearLayout) findViewById(R.id.detectingResultLayout);

        //createImageView();
        getRect = (Button) findViewById(R.id.getRect);
        getRect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rect rect = Imgproc.boundingRect(matOfPoints.get(0));
                Mat mat = new Mat(img_gray, rect);
                getPixcel(mat);
            }
        });


        if (!hasPermissions(PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            requestNecessaryPermissions(PERMISSIONS);//퍼미션 허가안되어 있다면 사용자에게 요청
        } else {
            //이미 사용자에게 퍼미션 허가를 받음.
            read_image_file(filename);
            imageprocess_and_showResult();
        }
        //CheckTypesTask task = new CheckTypesTask();
        //task.execute();
    }

    private void imageprocess_and_showResult() {

        imageprocessing(img_input.getNativeObjAddr(), img_output.getNativeObjAddr(), img_gray.getNativeObjAddr());
        //ImageSharpening(img_input.getNativeObjAddr(), img_gray.getNativeObjAddr(), img_output.getNativeObjAddr());

        //matOfPoints = SquareDetector.detectBiggestSquare(img_input);
        matOfPoints = (List<MatOfPoint>) SquareDetector.detectBiggestSquare(img_input);
        Imgproc.drawContours(img_input, matOfPoints, -1, new Scalar(0, 255, 0) ,2);

        //makeRoi(img_gray, matOfPoints);

        //makeRoi(img_gray, matOfPoints.get(0), 0);
        //makeRoi(img_gray, matOfPoints.get(2), 1);
        //makeRoi(img_gray, matOfPoints.get(4), 2);
        //makeRoi(img_gray, matOfPoints.get(6), 3);
        //Bitmap bitmapInput = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(img, bitmapInput);

        Bitmap bitmapInput = Bitmap.createBitmap(img_gray.cols(), img_gray.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_gray, bitmapInput);
        imageVIewOuput.setImageBitmap(bitmapInput);

        Bitmap bitmapOutput = Bitmap.createBitmap(img_input.cols(), img_input.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_input, bitmapOutput);
        imageVIewInput.setImageBitmap(bitmapOutput);
       // new ProgressDlgTest().execute(100);
    }
    private void  makeRoi(Mat img, List<MatOfPoint> points) {
        for (int i = 0; i < points.size(); i++) {
            if (i % 2 == 0) {
                Rect rect = Imgproc.boundingRect(points.get(i));
                Mat mat = new Mat(img, rect);
                createImageView(mat, i);
            }
        }
    }
       //Bitmap bmp_result = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
       //Utils.matToBitmap(mat, bmp_result);
        /*if(num == 0){
            whiteRed = getPixcel(mat).get(0);
            whiteGreen = getPixcel(mat).get(1);
            whiteBlue = getPixcel(mat).get(2);
            //result1.setVisibility(View.VISIBLE);
            //result1.setBackgroundColor(Color.WHITE);
        }else if(num == 1){
            //result2.setVisibility(View.VISIBLE);
            //result2.setBackgroundColor(Color.rgb((getPixcel(mat).get(0) - whiteRed), (getPixcel(mat).get(1) - whiteGreen), (getPixcel(mat).get(2) - whiteBlue)));
        }else if(num == 2){
            //result3.setVisibility(View.VISIBLE);
            //result3.setBackgroundColor(Color.rgb((getPixcel(mat).get(0) - whiteRed), (getPixcel(mat).get(1) - whiteGreen), (getPixcel(mat).get(2) - whiteBlue)));
        }else if(num == 3){
            //result4.setVisibility(View.VISIBLE);
            //result4.setBackgroundColor(Color.rgb((getPixcel(mat).get(0) - whiteRed), (getPixcel(mat).get(1) - whiteGreen), (getPixcel(mat).get(2) - whiteBlue)));
        }*/
   // }

    private void read_image_file(String filename) {
        //copyFile(filename);

        img_input = new Mat();
        img_output = new Mat();
        img_gray = new Mat();

        loadImage(filename, img_input.getNativeObjAddr());
        img_gray = img_input.clone();
        //ImageSharpening(img_input.getNativeObjAddr(), img_gray.getNativeObjAddr(), img_output.getNativeObjAddr());

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


    public void createImageView(Mat mat, int num){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(60, 60);
        lp.setMargins(5,5,5,5);
        ImageView imageView = new ImageView(UrineDetector.this);
        imageView.setLayoutParams(lp);
        if(num == 0){
            whiteRed = getPixcel(mat).get(0);
            whiteGreen = getPixcel(mat).get(1);
            whiteBlue = getPixcel(mat).get(2);
            imageView.setBackgroundColor(Color.WHITE);
        }else{
            //imageView.setBackgroundColor(Color.rgb((getPixcel(mat).get(0)), (getPixcel(mat).get(1)), (getPixcel(mat).get(2))));
            imageView.setBackgroundColor(Color.rgb((getPixcel(mat).get(0) - whiteRed), (getPixcel(mat).get(1) - whiteGreen), (getPixcel(mat).get(2) - whiteBlue)));
        }
        detectingResultLayout.addView(imageView);
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void loadImage(String imageFileName, long img);
    public native void imageprocessing(long inputImage, long outputImage, long outputImageGray);
    public native void ImageSharpening(long matAddrInput, long matGray, long matAddrResult);

    private class CheckTypesTask extends AsyncTask<Void, Void, Void> {

        ProgressDialog asyncDialog = new ProgressDialog(UrineDetector.this);

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("진단중입니다...");
            //show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

            @Override
            protected Void doInBackground(Void... arg0) {
                try {
                    for (int i = 0; i < 5; i++) {
                        //asyncDialog.setProgress(i * 30);
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                asyncDialog.dismiss();
                createImageView();
                super.onPostExecute(result);
        }
    }
}


