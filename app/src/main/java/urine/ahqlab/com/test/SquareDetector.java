package urine.ahqlab.com.test;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

// Based on OpenCV4Android 3.1
public class SquareDetector {

    /**
     * 사각형 검출을 위한 Iteration 횟수
     */
    public static int REPEAT = 11;
    //public static int REPEAT = 3;

    /**
     * 벡터에서 코사인 값을 구합니다.
     *
     * @param pt1 포인트 1
     * @param pt2 포인트 2
     * @param pt0 포인트 0
     * @return 두 각 사이의 코사인 값
     */
    private static double angle (Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;

        return (dx1 * dx2 + dy1 * dy2) /
                Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    /**
     * Approximates a polygonal curve with the specified precision. (like Python API)
     *
     * @param curve Input vector of a 2D point stored in Mat
     * @param epsilon Parameter specifying the approximation accuracy.
     *                (This is the maximum distance between the original curve and its approximation.)
     * @param closed If true, the approximated curve is closed. Otherwise, it is not closed.
     * @return Result of the approximation.
     */
    private static MatOfPoint approxPolyDP (MatOfPoint curve, double epsilon, boolean closed) {
        MatOfPoint2f tempMat = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(curve.toArray()), tempMat, epsilon, closed);
        return new MatOfPoint(tempMat.toArray());
    }
    /**
     * 라벨 표시
     */
    public static void setLabel(Mat im, String label, MatOfPoint contour)
    {
        int fontface = 3;
        double scale = 0.9;
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

    /**
     * 이미지에서 사각형을 검출합니다.
     *
     * @param image 검출을 위한 이미지
     * @return 사각형의 목록
     */


    private static List<MatOfPoint> detectSquareList (Mat image) {
        ArrayList<MatOfPoint> squares = new ArrayList<>();

        Mat smallerImg = new Mat(new Size(image.width() / 2, image.height() / 2), image.type());
        Mat gray0 = new Mat(image.size(), CvType.CV_8U);

        // 노이즈를 제거하기 위해 다운스케일한 뒤 업스케일합니다.
        Imgproc.pyrDown(image, smallerImg, smallerImg.size());
        Imgproc.pyrUp(smallerImg, image, image.size());

        // 모든 색상면에서 사각형을 찾습니다.
        for (int c = 0; c < 3; c++) {
            Mat gray = new Mat(image.size(), image.type());
            Core.extractChannel(image, gray, c);

            // 여러 수치로 테스트해봅니다.
            for (int l = 1; l < REPEAT; l++) {
               // int a = (l+1) * 255 / REPEAT;
                Imgproc.threshold(gray, gray0, (l+1) * 255 / REPEAT, 255, Imgproc.THRESH_BINARY);
                //Imgproc.threshold(gray, gray0, 231, 255,  Imgproc.THRESH_BINARY);

                // 검출된 모든 윤곽선들을 list로 저장합니다.
                List<MatOfPoint> contours = new ArrayList<>();
                Imgproc.findContours(gray0, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                Log.e("threshold", "contours.size() : " + String.valueOf(contours.size()));

                // 검출된 윤곽선이 사각형의 윤곽선인지 찾습니다.
                for (int i = 0; i < contours.size(); i++) {

                    // 윤곽선의 주변에 비례하여 대략적인 윤곽을 구합니다.
                    double epsilon = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true) * 0.02;
                    MatOfPoint approx = approxPolyDP(contours.get(i), epsilon, true);

                    // 사각형 윤곽선은 주변의 영역에 4개의 꼭짓점을 가지고 있어야합니다
                    // 주의 : 절대값이 쓰이는 이유는 윤곽선의 방향에 따라 영역의 값이 달라지기 때문입니다.
                    if (approx.toArray().length == 4 && Math.abs(Imgproc.contourArea(approx)) > 1000 && Imgproc.isContourConvex(approx)) {

                        // 인접한 꼭짓점들로 만들어진 각들 사이에서 최대 코사인 값을 구합니다.
                        double maxCosine = 0;
                        for (int j = 2; j < 5; j++)
                            maxCosine = Math.max(maxCosine, Math.abs(angle(approx.toArray()[j % 4],
                                    approx.toArray()[j - 2], approx.toArray()[j - 1])));

                        // 모든 각도의 코사인이 작을 경우 사각형으로 추가합니다.
                        if (maxCosine < 0.3) squares.add(approx);
                    }
                }
            }
        }

        return squares;
    }


    private static List<MatOfPoint> detectSquareList2(Mat matInput) {
        ArrayList<MatOfPoint> squares = new ArrayList<>();

        //Mat smallerImg = new Mat(new Size(image.width() / 2, image.height() / 2), image.type());
        Mat matResult = new Mat(matInput.size(), CvType.CV_8U);

        Imgproc.cvtColor(matInput, matResult, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(matResult, matResult, new Size(5, 5), 0);
        Imgproc.Canny(matResult, matResult, 75, 200);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();

        Imgproc.findContours(matResult, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

        Imgproc.threshold(matResult, matResult, 231, 255,  Imgproc.THRESH_BINARY);

        matResult = matInput.clone();

        List<MatOfPoint> rects = new ArrayList<MatOfPoint>();

        //for (int i = 0; i < contours.size(); i++) {
        for (int i = 0; i <  contours.size(); i++) {
            //Log.e("<<<<<<<<<<<<<","<<<<<<<<");
            double epsilon = Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true) * 0.02;
            MatOfPoint approx = approxPolyDP(contours.get(i), epsilon, true);

            if (approx.toArray().length == 4 && Math.abs(Imgproc.contourArea(approx)) > 1000  && Imgproc.isContourConvex(approx)) {
                double maxCosine = 0;
                for (int j = 2; j < 5; ++j) {
                    maxCosine = Math.max(maxCosine, Math.abs(angle(approx.toArray()[j % 4], approx.toArray()[j - 2], approx.toArray()[j - 1])));
                }
                if (maxCosine < 0.3) {

                    squares.add(approx);
                }

            }
        }
        return squares;
    }


    /**
     * 이미지에서 제일 큰 사각형을 검출합니다.
     *
     * @param image 검출을 위한 이미지
     * @return 사각형
     */

    /*public static List<MatOfPoint> detectBiggestSquare (Mat image) {
        List<MatOfPoint> squares = detectSquareList2(image);
        Imgproc.drawContours(image, squares, -1, new Scalar(0, 255, 0) ,2);
        MatOfPoint point = null;
        int i = 0;
        for (MatOfPoint square: squares){
            setLabel(image, String.valueOf(i), square);
            i++;
        }
        return squares;
    }*/

    public static List<MatOfPoint> detectBiggestSquare (Mat image) {
        List<MatOfPoint> squares = detectSquareList(image);
        ArrayList<MatOfPoint> retVal = new ArrayList<>();
        MatOfPoint point = null;

        for (MatOfPoint square: squares)
            if (point == null) point = square;
            else if (Math.abs(Imgproc.contourArea(point)) < Math.abs(Imgproc.contourArea(square)))
                point = square;

        if (point != null) retVal.add(point);
        return retVal;
    }
}


