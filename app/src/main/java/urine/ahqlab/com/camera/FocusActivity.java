package urine.ahqlab.com.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import urine.ahqlab.com.test.R;

public class FocusActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 , View.OnTouchListener {

    private static final String TAG = "test";

    private JavaCamResView mOpenCvCameraView;
    private List<Camera.Size> mResolutionList;

    private MenuItem[] mResolutionMenuItems;
    private MenuItem[] mFocusListItems;
    private MenuItem[] mFlashListItems;

    private SubMenu mResolutionMenu;
    private SubMenu mFocusMenu;
    private SubMenu mFlashMenu;

    private Mat mGrayMat;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setOnTouchListener(FocusActivity.this);
                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FocusActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.java_cam_res_view);

        mOpenCvCameraView = (JavaCamResView) findViewById(R.id.test_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);


    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

        mGrayMat = new Mat(height, width, CvType.CV_8UC1);

    }

    public void onCameraViewStopped() {
        mGrayMat.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mGrayMat=inputFrame.gray();
        return mGrayMat;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<String> mFocusList = new LinkedList<String>();
        int idx =0;

        mFocusMenu = menu.addSubMenu("Focus");

        mFocusList.add("Auto");
        mFocusList.add("Continuous Video");
        mFocusList.add("EDOF");
        mFocusList.add("Fixed");
        mFocusList.add("Infinity");
        mFocusList.add("Makro");
        mFocusList.add("Continuous Picture");

        mFocusListItems = new MenuItem[mFocusList.size()];

        ListIterator<String> FocusItr = mFocusList.listIterator();
        while(FocusItr.hasNext()){
            // add the element to the mDetectorMenu submenu
            String element = FocusItr.next();
            mFocusListItems[idx] = mFocusMenu.add(2,idx,Menu.NONE,element);
            idx++;
        }



        List<String> mFlashList = new LinkedList<String>();
        idx = 0;

        mFlashMenu = menu.addSubMenu("Flash");

        mFlashList.add("Auto");
        mFlashList.add("Off");
        mFlashList.add("On");
        mFlashList.add("Red-Eye");
        mFlashList.add("Torch");

        mFlashListItems = new MenuItem[mFlashList.size()];

        ListIterator<String> FlashItr = mFlashList.listIterator();
        while(FlashItr.hasNext()){
            // add the element to the mDetectorMenu submenu
            String element = FlashItr.next();
            mFlashListItems[idx] = mFlashMenu.add(3,idx,Menu.NONE,element);
            idx++;
        }



        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Camera.Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Camera.Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(1, idx, Menu.NONE,
                    Integer.valueOf((int) element.width).toString() + "x" + Integer.valueOf((int) element.height).toString());
            idx++;
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.e(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            int id = item.getItemId();
            Camera.Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            Log.e("test","test");
            String caption = Integer.valueOf((int) resolution.width).toString() + "x" + Integer.valueOf((int) resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId()==2){

            int focusType = item.getItemId();
            //String caption = "Focus Mode: "+ (String)item.getTitle();
            //Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();

            mOpenCvCameraView.setFocusMode(this, focusType);
        }
        else if (item.getGroupId()==3){

            int flashType = item.getItemId();
            //String caption = "Flash Mode: "+ (String)item.getTitle();
            //Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();

            mOpenCvCameraView.setFlashMode(this, flashType);
        }

        return true;
    }

    public boolean onTouch(View view, MotionEvent event) {
     /*   int xpos, ypos;

        xpos = (view.getWidth() - mGameWidth) / 2;
        xpos = (int)event.getX() - xpos;

        ypos = (view.getHeight() - mGameHeight) / 2;
        ypos = (int)event.getY() - ypos;

        if (xpos >=0 && xpos <= mGameWidth && ypos >=0  && ypos <= mGameHeight) {
            *//* click is inside the picture. Deliver this event to processor *//*
            mPuzzle15.deliverTouchEvent(xpos, ypos);
        }*/

        return false;
    }
}
