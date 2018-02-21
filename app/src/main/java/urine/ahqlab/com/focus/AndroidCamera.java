package urine.ahqlab.com.focus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import urine.ahqlab.com.test.R;

public class AndroidCamera extends Activity implements SurfaceHolder.Callback{

    Camera camera;
    CameraSurfaceView cameraSurfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    LayoutInflater controlInflater = null;

    Button buttonTakePicture;
    TextView prompt;

    DrawingView drawingView;
    Face[] detectedFaces;

    final int RESULT_SAVEIMAGE = 0;

    String filename;
    String sendFilename;

    private ScheduledExecutorService myScheduledExecutorService;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_camera);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        cameraSurfaceView = (CameraSurfaceView)findViewById(R.id.camerapreview);
        surfaceHolder = cameraSurfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        drawingView = new DrawingView(this);
        LayoutParams layoutParamsDrawing = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        this.addContentView(drawingView, layoutParamsDrawing);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, null);
        LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);

        buttonTakePicture = (Button)findViewById(R.id.takepicture);
        buttonTakePicture.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                camera.takePicture(myShutterCallback,
                        myPictureCallback_RAW, myPictureCallback_JPG);
            }});

        /*
        LinearLayout layoutBackground = (LinearLayout)findViewById(R.id.background);
        layoutBackground.setOnClickListener(new LinearLayout.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				buttonTakePicture.setEnabled(false);
				camera.autoFocus(myAutoFocusCallback);
			}});
		*/

        prompt = (TextView)findViewById(R.id.prompt);
    }

    public void touchFocus(final int posX, final int posY){

        buttonTakePicture.setEnabled(false);

        camera.stopFaceDetection();

//    	//Convert from View's width and height to +/- 1000
//		final Rect targetFocusRect = new Rect(
//				tfocusRect.left * 2000/drawingView.getWidth() - 1000,
//				tfocusRect.top * 2000/drawingView.getHeight() - 1000,
//				tfocusRect.right * 2000/drawingView.getWidth() - 1000,
//				tfocusRect.bottom * 2000/drawingView.getHeight() - 1000);
//
//		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
//		Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
//		focusList.add(focusArea);
//
//		Parameters para = camera.getParameters();
//		para.setFocusAreas(focusList);
//		para.setMeteringAreas(focusList);
//		camera.setParameters(para);

        setAutoFocusArea(camera, posX, posY, 128, true, new Point(cameraSurfaceView.getWidth(), cameraSurfaceView.getHeight()));

        camera.autoFocus(myAutoFocusCallback);

//		drawingView.setHaveTouch(true, tfocusRect);
//  		drawingView.invalidate();
    }

    private void setAutoFocusArea(Camera camera, int posX, int posY, int focusRange, boolean flag, Point point) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            /** 영역을 지정해서 포커싱을 맞추는 기능은 ICS 이상 버전에서만 지원됩니다.  **/
            return;
        }

        if (posX < 0 || posY < 0) {
            setArea(camera, null);
            return;
        }

        int touchPointX;
        int touchPointY;
        int endFocusY;
        int startFocusY;

        if (!flag) {
            /** Camera.setDisplayOrientation()을 이용해서 영상을 세로로 보고 있는 경우. **/
            touchPointX = point.y >> 1;
            touchPointY = point.x >> 1;

            startFocusY = posX;
            endFocusY 	= posY;
        } else {
            /** Camera.setDisplayOrientation()을 이용해서 영상을 가로로 보고 있는 경우. **/
            touchPointX = point.x >> 1;
            touchPointY = point.y >> 1;

            startFocusY = posY;
            endFocusY = point.x - posX;
        }

        float startFocusX 	= 1000F / (float) touchPointY;
        float endFocusX 	= 1000F / (float) touchPointX;

        startFocusX = (int) (startFocusX * (float) (startFocusY - touchPointY)) - focusRange;
        startFocusY = (int) (endFocusX * (float) (endFocusY - touchPointX)) - focusRange;
        endFocusX = startFocusX + focusRange;
        endFocusY = startFocusY + focusRange;

        if (startFocusX < -1000)
            startFocusX = -1000;

        if (startFocusY < -1000)
            startFocusY = -1000;

        if (endFocusX > 1000) {
            endFocusX = 1000;
        }

        if (endFocusY > 1000) {
            endFocusY = 1000;
        }

        Rect rect = new Rect((int) startFocusX, (int) startFocusY, (int) endFocusX, (int) endFocusY);
        ArrayList<Camera.Area> arraylist = new ArrayList<Camera.Area>();
        arraylist.add(new Camera.Area(rect, 1000));

        setArea(camera, arraylist);
    }

    private void setArea(Camera camera, List<Camera.Area> list) {
        Camera.Parameters parameters;
        parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() > 0) {
            parameters.setFocusAreas(list);
        }

        if (parameters.getMaxNumMeteringAreas() > 0) {
            parameters.setMeteringAreas(list);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(parameters);
    }

    FaceDetectionListener faceDetectionListener
            = new FaceDetectionListener(){

        @Override
        public void onFaceDetection(Face[] faces, Camera tcamera) {

            if (faces.length == 0){
                //prompt.setText(" No Face Detected! ");
                drawingView.setHaveFace(false);
            }else{
                //prompt.setText(String.valueOf(faces.length) + " Face Detected :) ");
                drawingView.setHaveFace(true);
                detectedFaces = faces;

                //Set the FocusAreas using the first detected face
                List<Camera.Area> focusList = new ArrayList<Camera.Area>();
                Camera.Area firstFace = new Camera.Area(faces[0].rect, 1000);
                focusList.add(firstFace);

                Parameters para = camera.getParameters();

                if(para.getMaxNumFocusAreas()>0){
                    para.setFocusAreas(focusList);
                }

                if(para.getMaxNumMeteringAreas()>0){
                    para.setMeteringAreas(focusList);
                }

                camera.setParameters(para);

                buttonTakePicture.setEnabled(false);

                //Stop further Face Detection
                camera.stopFaceDetection();

                buttonTakePicture.setEnabled(false);

				/*
				 * Allways throw java.lang.RuntimeException: autoFocus failed
				 * if I call autoFocus(myAutoFocusCallback) here!
				 *
					camera.autoFocus(myAutoFocusCallback);
				*/

                //Delay call autoFocus(myAutoFocusCallback)
                myScheduledExecutorService = Executors.newScheduledThreadPool(1);
                myScheduledExecutorService.schedule(new Runnable(){
                    public void run() {
                        camera.autoFocus(myAutoFocusCallback);
                    }
                }, 500, TimeUnit.MILLISECONDS);

            }

            drawingView.invalidate();

        }};

    AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback(){

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            // TODO Auto-generated method stub
            if (arg0){
                buttonTakePicture.setEnabled(true);
                camera.cancelAutoFocus();
            }

            float focusDistances[] = new float[3];
            arg1.getParameters().getFocusDistances(focusDistances);
            prompt.setText("Optimal Focus Distance(meters): "
                    + focusDistances[Camera.Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX]);

        }};

    ShutterCallback myShutterCallback = new ShutterCallback(){

        @Override
        public void onShutter() {
            // TODO Auto-generated method stub

        }};

    PictureCallback myPictureCallback_RAW = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            // TODO Auto-generated method stub

        }};

    PictureCallback myPictureCallback_JPG = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            String sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            filename =  "/test/" + (int) System.currentTimeMillis() + ".png";
            sendFilename =  "DCIM/test/" + (int) System.currentTimeMillis() + ".png";
            String path = sd + filename;

            Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);


            camera.startPreview();
            camera.startFaceDetection();
            saveImage(path, bitmapPicture);
        }
    };

    private void saveImage(String path, Bitmap bitmap){
        File file = new File(path);
        OutputStream out;
        try {
            file.createNewFile();
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG , 100, out);
            out.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        Toast.makeText(AndroidCamera.this, path, Toast.LENGTH_LONG).show();

        goDetectingPage();
    }

    private void goDetectingPage() {
        Intent intent = new Intent(AndroidCamera.this, UrineDetector.class);
        intent.putExtra("filename", sendFilename);
        startActivity(intent);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(previewing){
            camera.stopFaceDetection();
            camera.stopPreview();
            previewing = false;
        }
        if (camera != null){
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();

                prompt.setText(String.valueOf(
                        "Max Face: " + camera.getParameters().getMaxNumDetectedFaces()));
                camera.startFaceDetection();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        camera.setFaceDetectionListener(faceDetectionListener);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopFaceDetection();
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    private class DrawingView extends View{

        boolean haveFace;
        Paint drawingPaint;

        boolean haveTouch;
        Rect touchArea;

        public DrawingView(Context context) {
            super(context);
            haveFace = false;
            drawingPaint = new Paint();
            drawingPaint.setColor(Color.GREEN);
            drawingPaint.setStyle(Paint.Style.STROKE);
            drawingPaint.setStrokeWidth(2);

            haveTouch = false;
        }

        public void setHaveFace(boolean h){
            haveFace = h;
        }

        public void setHaveTouch(boolean t, Rect tArea){
            haveTouch = t;
            touchArea = tArea;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if(haveFace){
                // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
                // UI coordinates range from (0, 0) to (width, height).
                int vWidth = getWidth();
                int vHeight = getHeight();
                for(int i=0; i<detectedFaces.length; i++){
                    if(i == 0){
                        drawingPaint.setColor(Color.GREEN);
                    }else{
                        drawingPaint.setColor(Color.RED);
                    }
                    int l = detectedFaces[i].rect.left;
                    int t = detectedFaces[i].rect.top;
                    int r = detectedFaces[i].rect.right;
                    int b = detectedFaces[i].rect.bottom;
                    int left	= (l+1000) * vWidth/2000;
                    int top		= (t+1000) * vHeight/2000;
                    int right	= (r+1000) * vWidth/2000;
                    int bottom	= (b+1000) * vHeight/2000;
                    canvas.drawRect(
                            left, top, right, bottom,
                            drawingPaint);
                }
            }else{
                canvas.drawColor(Color.TRANSPARENT);
            }

            if(haveTouch){
                drawingPaint.setColor(Color.BLUE);
                canvas.drawRect(
                        touchArea.left, touchArea.top, touchArea.right, touchArea.bottom,
                        drawingPaint);
            }
        }

    }
}

