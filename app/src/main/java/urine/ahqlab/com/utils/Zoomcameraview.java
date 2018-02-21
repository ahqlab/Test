package urine.ahqlab.com.utils;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by silve on 2017-11-16.
 */

public class Zoomcameraview extends JavaCameraView {
    public Zoomcameraview(
            Context context, int cameraId) {
        super(context, cameraId);
    }

    public Zoomcameraview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected SeekBar seekBar;

    protected Button focusButton;

    public void setZoomControl(SeekBar _seekBar) {
        seekBar = _seekBar;
    }

    public void setFocusControl(Button _button) {
        focusButton = _button;
    }

    protected void enableZoomControls(Camera.Parameters params) {

        final int maxZoom = params.getMaxZoom();
        seekBar.setMax(maxZoom);
        seekBar.setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {
                    int progressvalue = 0;

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        // TODO Auto-generated method stub
                        progressvalue = progress;
                        Camera.Parameters params = mCamera.getParameters();
                        params.setZoom(progress);

                        mCamera.setParameters(params);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub

                    }


                }

        );



        focusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    public void onAutoFocus(boolean success, Camera camera) {
                        //  Toast.makeText(context,)
                        if (success) {
                            mCamera.takePicture(null, null, null);
                        }
                    }
                });
            }

        });

    }

    private Camera.AutoFocusCallback mFocusListener = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
            }else{

            }
        }
    };
    protected boolean initializeCamera(int width, int height) {

        boolean ret = super.initializeCamera(width, height);
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        //Camera.Size size =  getOptimalPictureSize(params.getSupportedPictureSizes(), 1280, 720);
        //params.setPreviewSize(size.width,  size.height);
        //params.setPictureSize(size.width,  size.height);
        if (params.isZoomSupported())
            enableZoomControls(params);
        mCamera.setParameters(params);
        //mCamera.release();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
            }
        });
        return ret;
    }

    private Camera.Size getOptimalPictureSize(List<Camera.Size> sizeList, int width, int height){
       // Log.d(TAG, "getOptimalPictureSize, 기준 width,height : (" + width + ", " + height + ")");
        Camera.Size prevSize = sizeList.get(0);
        Camera.Size optSize = sizeList.get(1);
        for(Camera.Size size : sizeList){
            // 현재 사이즈와 원하는 사이즈의 차이
            int diffWidth = Math.abs((size.width - width));
            int diffHeight = Math.abs((size.height - height));

            // 이전 사이즈와 원하는 사이즈의 차이
            int diffWidthPrev = Math.abs((prevSize.width - width));
            int diffHeightPrev = Math.abs((prevSize.height - height));

            // 현재까지 최적화 사이즈와 원하는 사이즈의 차이
            int diffWidthOpt = Math.abs((optSize.width - width));
            int diffHeightOpt = Math.abs((optSize.height - height));

            // 이전 사이즈보다 현재 사이즈의 가로사이즈 차이가 적을 경우 && 현재까지 최적화 된 세로높이 차이보다 현재 세로높이 차이가 적거나 같을 경우에만 적용
            if(diffWidth < diffWidthPrev && diffHeight <= diffHeightOpt){
                optSize = size;
               // Log.d(TAG, "가로사이즈 변경 / 기존 가로사이즈 : " + prevSize.width + ", 새 가로사이즈 : " + optSize.width);
            }
            // 이전 사이즈보다 현재 사이즈의 세로사이즈 차이가 적을 경우 && 현재까지 최적화 된 가로길이 차이보다 현재 가로길이 차이가 적거나 같을 경우에만 적용
            if(diffHeight < diffHeightPrev && diffWidth <= diffWidthOpt){
                optSize = size;
                //Log.d(TAG, "세로사이즈 변경 / 기존 세로사이즈 : " + prevSize.height + ", 새 세로사이즈 : " + optSize.height);
            }

            // 현재까지 사용한 사이즈를 이전 사이즈로 지정
            prevSize = size;
        }
        //Log.d(TAG, "결과 OptimalPictureSize : " + optSize.width + ", " + optSize.height);
        return optSize;
    }

}