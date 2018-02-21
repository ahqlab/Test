package urine.ahqlab.com.test;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.Serializable;
import java.util.List;

public class DetectResultActivity extends AppCompatActivity implements Serializable{


    private ImageView result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_result);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        //Bitmap image = (Bitmap)intent.getParcelableExtra("image");
        //List<MatOfPoint> list = (List<MatOfPoint>) getIntent().getSerializableExtra("imageList");

        List<MatOfPoint> list = (List<MatOfPoint>)bundle.getSerializable("imageList");
        Log.e("size", String.valueOf(list.size()));


        long addr = intent.getLongExtra("image", 0);
        Mat tempImg = new Mat(addr);
        Mat img = tempImg.clone();


        result = (ImageView) findViewById(R.id.result);
        Rect rect = Imgproc.boundingRect(list.get(0));
        Mat mat = new Mat(img, rect);
        Bitmap bmp_result = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp_result);
        result.setImageBitmap(bmp_result);

        //representativeColor(bmp_result);
        //result.setVisibility(View.VISIBLE);
        //result.setImageBitmap(bmp_result);
    }
}
