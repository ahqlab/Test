package urine.ahqlab.com.test;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import urine.ahqlab.com.utils.Zoomcameraview;



public class CustomActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2  {

    public Zoomcameraview zoomcameraview;

    private Mat mRgba;

    public Button button;

    private OrientationEventListener mOrientEventListener;

    private SurfaceView mSurfaceRoi;
    private SurfaceView mSurfaceRoiBorder;

    private int mRoiWidth;
    private int mRoiHeight;
    private int mRoiX;
    private int mRoiY;
    private double m_dWscale;
    private double m_dHscale;

    //네모칸들
    private Rect rect;
    private Rect mRectRoi2;
    private Rect mRectRoi3;
    private Rect mRectRoi4;
    private Rect mRectRoi5;
    private Rect mRectRoi6;
    private Rect mRectRoi7;
    private Rect mRectRoi8;
    private Rect mRectRoi9;
    private Rect mRectRoi10;
    private Rect mRectRoi11;

    private Mat m_matRoi1;
    private Mat m_matRoi2;
    private Mat m_matRoi3;
    private Mat m_matRoi4;
    private Mat m_matRoi5;
    private Mat m_matRoi6;
    private Mat m_matRoi7;
    private Mat m_matRoi8;
    private Mat m_matRoi9;
    private Mat m_matRoi10;
    private Mat m_matRoi11;

    private Point point1;

    // 현재 회전 상태 (하단 Home 버튼의 위치)

    private android.widget.RelativeLayout.LayoutParams mRelativeParams;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    zoomcameraview.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom);

        mSurfaceRoi = (SurfaceView) findViewById(R.id.surface_roi);
        mSurfaceRoiBorder = (SurfaceView) findViewById(R.id.surface_roi_border);

        zoomcameraview = (Zoomcameraview)findViewById(R.id.ZoomCameraView);
        zoomcameraview.setVisibility(SurfaceView.VISIBLE);
        zoomcameraview.setZoomControl((SeekBar) findViewById(R.id.CameraZoomControls));
        zoomcameraview.setFocusControl((Button) findViewById(R.id.btn_ocrstart));
        zoomcameraview.setCvCameraViewListener(this);

        mOrientEventListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {

            @Override
            public void onOrientationChanged(int arg0) {

                /*if (arg0 >= 315 || arg0 < 45) {
                    rotateViews(270);
                    // 90˚
                } else if (arg0 >= 45 && arg0 < 135) {
                    rotateViews(180);
                    // 180˚
                } else if (arg0 >= 135 && arg0 < 225) {
                    rotateViews(90);
                    // 270˚ (landscape)
                } else {
                    rotateViews(0);
                }
                //ROI 선 조정
                mRelativeParams = new android.widget.RelativeLayout.LayoutParams(mRoiWidth + 5, mRoiHeight + 5);
                mRelativeParams.setMargins(mRoiX, mRoiY, 0, 0);
                mSurfaceRoiBorder.setLayoutParams(mRelativeParams);

                //ROI 영역 조정
                mRelativeParams = new android.widget.RelativeLayout.LayoutParams(mRoiWidth - 5, mRoiHeight - 5);
                mRelativeParams.setMargins(mRoiX + 5, mRoiY + 5, 0, 0);
                mSurfaceRoi.setLayoutParams(mRelativeParams);*/
            }
        };
        //방향센서 핸들러 활성화
        mOrientEventListener.enable();
        //방향센서 인식 오류 시, Toast 메시지 출력 후 종료
        if (!mOrientEventListener.canDetectOrientation()) {
            Toast.makeText(this, "Can't Detect Orientation", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (zoomcameraview!= null)
            zoomcameraview.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (zoomcameraview != null)
            zoomcameraview.disableView();
    }

    public void rotateViews(int degree) {
        switch (degree) {
            // 가로
            case 0:
            case 180:
                //ROI 크기 조정 비율 변경
                m_dWscale = (double) 1 / 2;
                m_dHscale = (double) 1 / 2;
                break;
            // 세로
            case 90:
            case 270:
                m_dWscale = (double) 1 / 4;    //h (반대)
                m_dHscale = (double) 3 / 4;    //w
                break;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,mLoaderCallback );
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
        // TODO Auto-generated method stub
    }
    @Override
    public void onCameraViewStopped() {
        // TODO Auto-generated method stub
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // 프레임 획득
        mRgba = inputFrame.rgba();
        Mat sharpen_image;
       /// Mat sharpen_kenel = (Mat<String>)

       // Imgproc.filt

        //Imgproc.rectangle(mRgba, new Point(100, 100), new Point(200, 200),new Scalar(0, 255, 0));
        //가로, 세로 사이즈 획득
        //Imgproc.line(mRgba, new Point(0, 100), new Point(0, 100), new Scalar(0,255,0), 3);
        //사이즈로 중심에 맞는 X , Y 좌표값 계산
        //mRoiX = (int) (img_input.size().width - mRoiWidth) / 2;
        //mRoiY = (int) (img_input.size().height - mRoiHeight) / 2;
        //ROI 영역 생성
        //Imgproc.rectangle(mRgba, mRectRoi1, new Scalar(0, 255, 0), 1,8,0);
        //Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),new Scalar(0, 255, 0));
        //ROI 영역 흑백으로 전환
        //m_matRoi1 = img_input.submat(mRectRoi1);
        //Imgproc.cvtColor(m_matRoi1, m_matRoi1, Imgproc.COLOR_RGBA2GRAY);
        //Imgproc.cvtColor(m_matRoi1, m_matRoi1, Imgproc.COLOR_GRAY2RGBA);
        //m_matRoi1.copyTo(img_input.submat(mRectRoi1));

        return mRgba;
    }
}