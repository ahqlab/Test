#include <jni.h>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>
#include <vector>
#include <opencv/highgui.h>
#include <opencv2/opencv.hpp>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#pragma
using namespace cv;
using namespace std;




void setLabel( Mat& image, string str, vector<Point> contour)
{
        int fontface = FONT_HERSHEY_SIMPLEX;
        double scale = 0.5;
        int thickness = 1;
        int baseline = 0;

        Size text = getTextSize(str, fontface, scale, thickness, &baseline);
        Rect r = boundingRect(contour);

        Point pt(r.x + ((r.width - text.width) / 2), r.y + ((r.height + text.height) / 2));
        rectangle(image, pt + Point(0, baseline), pt + Point(text.width, -text.height), CV_RGB(200, 200, 200), CV_FILLED);
        putText(image, str, pt, fontface, scale, CV_RGB(0, 0, 0), thickness, 8);
}

template <typename T>
std::string to_string(T const& value) {
    stringstream sstr;
    sstr << value;
    return sstr.str();
}

string convertInt(int number)
{
    stringstream ss;//create a stringstream
    ss << number;//add number to the stream
    return ss.str();//return a string with the contents of the stream
}


Mat Sharpen (const Mat& input, Mat& result){
    CV_Assert(input.depth() == CV_8U);

    int ONE = 5;

    result.create(input.size(), input.type());
    const int nChannels = input.channels();

    for (int y = 0; y < input.rows - 1; ++y) {
        const uchar* previous = input.ptr<uchar >(y - ONE);
        const uchar* current = input.ptr<uchar >(y    );
        const uchar* next = input.ptr<uchar >(y +  ONE);

        uchar* output = result.ptr<uchar >(y);

        for (int x = nChannels; x < nChannels * (input.cols -1); ++x) {
            *output++ = saturate_cast<uchar>(5 * current[x] -current[x - nChannels] - current[x + nChannels] - previous[x] - next[x]);
        }
    }
    result.row(0).setTo(Scalar(0));
    result.row(result.rows - 1).setTo(Scalar(0));
    result.col(0).setTo(Scalar(0));
    result.col(result.cols - 1).setTo(Scalar(0));
    return input;

//461389 00 058027
}

void cornerHarris_demo(int, void* , Mat& g_harris_src)
{
    Mat dst, dst_norm, dst_norm_scaled;
    dst = Mat::zeros(g_harris_src.size(), CV_32FC1);

    int blockSize = 2;
    int apertureSize = 3;
    double k = 0.06;

    cornerHarris(g_harris_src, dst, blockSize, apertureSize, k, BORDER_DEFAULT);

    normalize(dst, dst_norm, 0, 255, NORM_MINMAX, CV_32FC1, Mat());

    convertScaleAbs(dst_norm, dst_norm_scaled);

    for (int j = 0; j <dst_norm.rows; ++j) {
        for (int i = 0; i < dst_norm.cols; ++i) {
            if((int)dst_norm.at<float>(j, i) > 127){
                circle(dst_norm_scaled, Point(i, j), 5, Scalar(0), 2,8,0);
            }
        }
    }
}

int GetAngleABC(Point a, Point b, Point c)
{
        Point ab = { b.x - a.x, b.y - a.y };
        Point cb = { b.x - c.x, b.y - c.y };

        float dot = (ab.x * cb.x + ab.y * cb.y); // dot product
        float cross = (ab.x * cb.y - ab.y * cb.x); // cross product

        float alpha = atan2(cross, dot);

        return (int)floor(alpha * 180.0 / CV_PI + 0.5);
}

static double angle(Point pt1, Point pt2, Point pt0)
{
     double dx1 = pt1.x - pt0.x;
     double dy1 = pt1.y - pt0.y;
     double dx2 = pt2.x - pt0.x;
     double dy2 = pt2.y - pt0.y;
     return (dx1*dx2 + dy1*dy2)/sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
 }

void detectorRect(Mat& img_input, Mat& img_result) {

    cvtColor(img_input, img_result, CV_RGB2GRAY);
    GaussianBlur(img_result, img_result, Size(5, 5), 0);
    Canny(img_result, img_result, 75, 200);

    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;
    findContours(img_result, contours, hierarchy, CV_RETR_TREE, CV_CHAIN_APPROX_SIMPLE, Point(0, 0));

    img_result = img_input.clone();

    vector<vector<Point> > rects;
    for (int i = 0; i < contours.size(); ++i) {
        double peri = arcLength(contours[i], true);
        vector<Point> approx;
        approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true) * 0.02, true);

        if (approx.size() >= 4 && fabs(contourArea(Mat(approx))) > 100 && isContourConvex(Mat(approx))) {
            double maxCosine = 0;
            for (int j = 2; j < 5; ++j) {
                double cosine = fabs(angle(approx[j % 4], approx[j - 2], approx[j - 1]));
                maxCosine = MAX(maxCosine, cosine);
                setLabel(img_result, to_string(maxCosine), contours[i]);
            }
            rects.push_back(approx);
                if (maxCosine < 1.5) {

                 }
            }
            if(approx.size() != 0) approx.clear();
        }
        drawContours(img_result, rects, -1, Scalar(0, 255, 0) ,2);
}


extern "C"
{
    JNIEXPORT void JNICALL
    Java_urine_ahqlab_com_focus_UrineDetector_loadImage(
            JNIEnv *env,
            jobject,
            jstring imageFileName,
            jlong addrImage) {

        Mat &img_input = *(Mat *) addrImage;

        const char *nativeFileNameString = env->GetStringUTFChars(imageFileName, JNI_FALSE);

        string baseDir("/storage/emulated/0/");
        //baseDir.append("DCIM/test/950060416.png");
        baseDir.append(nativeFileNameString);
        const char *pathDir = baseDir.c_str();
        img_input = imread(pathDir, IMREAD_COLOR);

    }

    JNIEXPORT void JNICALL
    Java_urine_ahqlab_com_focus_UrineDetector_imageprocessing(
            JNIEnv *env,
            jobject,
            jlong addrInputImage,
            jlong addrOutputImage ,
            jlong addrOutputImageGray) {

        Mat &img_input = *(Mat *) addrInputImage;
        Mat &img_output = *(Mat *) addrOutputImage;
        Mat &img_gray = *(Mat *) addrOutputImageGray;

        //Imgproc.cvtColor(matInput, matResult, Imgproc.COLOR_RGBA2GRAY);
        //Imgproc.GaussianBlur(matResult, matResult, new Size(5, 5), 0);
        //Imgproc.Canny(matResult, matResult, 75, 200);

        cvtColor( img_input, img_input, CV_BGR2RGB);
        cvtColor( img_gray, img_gray, CV_BGR2RGB);
        cvtColor( img_input, img_output, CV_RGB2GRAY);
        blur( img_output, img_output, Size(5,5) );
        Canny( img_output, img_output, 5, 255, 5 );
        threshold(img_output, img_output, 5, 255, THRESH_BINARY_INV | THRESH_OTSU);
        //threshold(img_output, img_output, 125, 255, THRESH_BINARY_INV | THRESH_OTSU);
    }




    JNIEXPORT void JNICALL Java_urine_ahqlab_com_test_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject instance, jlong matAddrInput, jlong matGray, jlong matAddrResult) {

            Mat &matInput = *(Mat *) matAddrInput;
            Mat &matResult = *(Mat *) matAddrResult;


            //cvtColor(matInput, matInput, CV_RGBA2GRAY);

            //cv::Rect rect(Point(460,450), Point((460 + 1000),(450 + 180)));
            //rectangle(matInput, rect, CV_RGB(0, 255, 0), 2);
            //Mat roi = matInput(rect);

            //cvtColor(roi , matInput , CV_GRAY2BGR);

            //그레이스케일 이미지로 변환
            /* //이진화 이미지로 변환
            Mat binary_image;
            threshold(img_gray, img_gray, 125, 255, THRESH_BINARY_INV | THRESH_OTSU);

            //contour를 찾는다.
            vector<vector<Point> > contours;
            findContours(img_gray, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

            //contour를 근사화한다.
            vector<Point2f> approx;
            img_result = img_input.clone();


            vector<vector<Point> > rects;
            for (size_t i = 0; i < contours.size(); i++) {
                double peri = arcLength(contours[i], true);
                vector<Point> approx;
                approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true) * 0.02, true);

                if(approx.size() >= 4 && fabs(contourArea(Mat(approx))) > 100 && isContourConvex(Mat(approx))){
                    double maxCosine = 0;
                    for (int j = 0; j < 5; ++j) {
                        double cosine = fabs(GetAngleABC(approx[j%4], approx[j-2], approx[j-1]));
                        maxCosine = MAX(maxCosine , cosine);\


                    }
                    if(maxCosine < 0.3){
                        rects.push_back(approx);
                    }

                }
                *//*if (
                        fabs(contourArea(Mat(approx))) > 100  //면적이 일정크기 이상이어야 한다.
                        //isContourConvex(Mat(approx)) //convex인지 검사한다.
                        ) {
                        int size = approx.size();

                        //Contour를 근사화한 직선을 그린다.
                        if (size % 2 == 0) {
                                line(img_result, approx[0], approx[approx.size() - 1],
                                     Scalar(0, 255, 0), 3);
                                for (int k = 0; k < size - 1; k++)
                                     line(img_result, approx[k], approx[k + 1], Scalar(0, 255, 0), 3);
                                for (int k = 0; k < size; k++)
                                      circle(img_result, approx[k], 3, Scalar(0, 0, 255));
                        } else {
                                line(img_result, approx[0], approx[approx.size() - 1],  Scalar(0, 255, 0), 3);
                                for (int k = 0; k < size - 1; k++)
                                    line(img_result, approx[k], approx[k + 1], Scalar(0, 255, 0), 3);
                                for (int k = 0; k < size; k++)
                                    circle(img_result, approx[k], 3, Scalar(0, 0, 255));
                        }

                        //모든 코너의 각도를 구한다.
                        vector<int> angle;

                        cout << "===" << size << endl;
                        for (int k = 0; k < size; k++) {
                                int ang = GetAngleABC(approx[k], approx[(k + 1) % size], approx[(k + 2) % size]);
                                cout << k << k + 1 << k + 2 << "@@" << ang << endl;
                                angle.push_back(ang);
                        }
                        sort(angle.begin(), angle.end());

                        int minAngle = angle.front();
                        int maxAngle = angle.back();
                        int threshold = 8;

                        if (size == 3)
                                setLabel(img_result, "triangle", contours[i]);
                        else if (size == 4 && minAngle >= 90 - threshold && maxAngle <= 90 + threshold)
                                setLabel(img_result, "rectangle", contours[i]);
                        else if (size == 5 && minAngle >= 108 - threshold && maxAngle <= 108 + threshold)
                                setLabel(img_result, "pentagon", contours[i]);
                        else if (size == 6 && minAngle >= 120 - threshold && maxAngle <= 120 + threshold)
                                setLabel(img_result, "hexagon", contours[i]);
                        //else
                        //       setLabel(img_result, to_string(approx.size()), contours[i]);//알수 없는 경우에는 찾아낸 꼭지점 갯수를 표시
                }*//*
            }*/
            //return img_result;
    }

    JNIEXPORT void JNICALL Java_urine_ahqlab_com_test_MainActivity_CustomActivityFindView(JNIEnv *env, jobject instance, jlong matAddrInput, jlong matGray, jlong matAddrResult) {

        Mat &img_input = *(Mat *) matAddrInput;
        Mat &img_result = *(Mat *) matAddrResult;
        Mat &img_gray = *(Mat *) matGray;

        //Mat img_input, img_gray, img_result;

        //이미지 파일을 읽어와서 img_input에 저장
        //img_input = imread("input6.png", IMREAD_COLOR);
        //if (img_input.empty())
        //{1
        //      cout << "파일을 읽어올수 없습니다." << endl;
        //      exit(1);
        //}

        //입력영상을 그레이스케일 영상으로 변환
        //img_gray = Mat(img_input.rows, img_input.cols, CV_8UC1);
        img_gray = Mat(img_input.rows, img_input.cols, CV_8UC1);

        for (int y = 0; y < img_input.rows; y++)
        {
            for (int x = 0; x < img_input.cols; x++)
            {
                //img_input으로부터 현재 위치 (y,x) 픽셀의
                //blue, green, red 값을 읽어온다.
                uchar blue = img_input.at<Vec3b>(y, x)[0];
                uchar green = img_input.at<Vec3b>(y, x)[1];
                uchar red = img_input.at<Vec3b>(y, x)[2];

                //blue, green, red를 더한 후, 3으로 나누면 그레이스케일이 된다.
                uchar gray = (blue + green + red) / 3.0;

                //Mat타입 변수 img_gray에 저장한다.
                img_gray.at<uchar>(y, x) = gray;
            }
        }


        //라플라시안 마스크
        int mask1[3][3] = { {-1,-1,-1},  //에지 검출
                            { -1,8,-1 },
                            { -1,-1,-1 }};

        int mask2[3][3] = { { -1,-1,-1 }, //영상 선명하게
                            { -1,9,-1 },
                            { -1,-1,-1 } };


        long int sum;
        //img_result = Mat(img_input.rows, img_input.cols, CV_8UC1);
        int masksize = 3;

        for (int y = 0 + masksize / 2; y < img_input.rows - masksize / 2; y++)
        {
            for (int x = 0 + masksize / 2; x < img_input.cols - masksize / 2; x++)
            {
                sum = 0;
                for (int i = -1 * masksize / 2; i <= masksize / 2; i++)
                {
                    for (int j = -1 * masksize / 2; j <= masksize / 2; j++)
                    {
                        //sum += img_gray.at<uchar>(y + i, x + j) * mask1[masksize / 2 + i][masksize / 2 + j];
                        sum += img_gray.at<uchar>(y + i, x + j) * mask2[masksize / 2 + i][masksize / 2 + j];
                    }
                }

                //0~255 사이값으로 조정
                if (sum > 255) sum = 255;
                if (sum < 0) sum = 0;

                img_result.at<uchar>(y, x) = sum;
            }
        }
        //화면에 결과 이미지를 보여준다.
        //imshow("입력 영상", img_input);
        //imshow("입력 그레이스케일 영상", img_gray);
        //imshow("결과 영상", img_result);

        //아무키를 누르기 전까지 대기
        //while (cvWaitKey(0) == 0);

        //결과를 파일로 저장
        //imwrite("img_gray.jpg", img_gray);
        //imwrite("img_result.jpg", img_result);
    }


    JNIEXPORT void JNICALL Java_urine_ahqlab_com_focus_UrineDetector_ImageSharpening(JNIEnv *env, jobject instance, jlong matAddrInput, jlong matGray, jlong matAddrResult) {

        Mat &img_input = *(Mat *) matAddrInput;
        Mat &img_result = *(Mat *) matAddrResult;
        Mat &img_gray = *(Mat *) matGray;

        img_gray = Mat(img_input.rows, img_input.cols, CV_8UC1);

        for (int y = 0; y < img_input.rows; y++)
        {
            for (int x = 0; x < img_input.cols; x++)
            {
                //img_input으로부터 현재 위치 (y,x) 픽셀의
                //blue, green, red 값을 읽어온다.
                uchar blue = img_input.at<Vec3b>(y, x)[0];
                uchar green = img_input.at<Vec3b>(y, x)[1];
                uchar red = img_input.at<Vec3b>(y, x)[2];

                //blue, green, red를 더한 후, 3으로 나누면 그레이스케일이 된다.
                uchar gray = (blue + green + red) / 3.0;

                //Mat타입 변수 img_gray에 저장한다.
                img_gray.at<uchar>(y, x) = gray;
            }
        }


        //라플라시안 마스크
        int mask1[3][3] = { {-1,-1,-1},  //에지 검출
                            { -1,8,-1 },
                            { -1,-1,-1 }};

        int mask2[3][3] = { { -1,-1,-1 }, //영상 선명하게
                            { -1,9,-1 },
                            { -1,-1,-1 } };


        long int sum;
        //img_result = Mat(img_input.rows, img_input.cols, CV_8UC1);
        int masksize = 3;

        for (int y = 0 + masksize / 2; y < img_input.rows - masksize / 2; y++)
        {
            for (int x = 0 + masksize / 2; x < img_input.cols - masksize / 2; x++)
            {
                sum = 0;
                for (int i = -1 * masksize / 2; i <= masksize / 2; i++)
                {
                    for (int j = -1 * masksize / 2; j <= masksize / 2; j++)
                    {
                        //sum += img_gray.at<uchar>(y + i, x + j) * mask1[masksize / 2 + i][masksize / 2 + j];
                        sum += img_gray.at<uchar>(y + i, x + j) * mask2[masksize / 2 + i][masksize / 2 + j];
                    }
                }

                //0~255 사이값으로 조정
                if (sum > 255) sum = 255;
                if (sum < 0) sum = 0;

                img_result.at<uchar>(y, x) = sum;
            }
        }
        //화면에 결과 이미지를 보여준다.
        //imshow("입력 영상", img_input);
        //imshow("입력 그레이스케일 영상", img_gray);
        //imshow("결과 영상", img_result);

        //아무키를 누르기 전까지 대기
        //while (cvWaitKey(0) == 0);

        //결과를 파일로 저장
        //imwrite("img_gray.jpg", img_gray);
        //imwrite("img_result.jpg", img_result);
    }

    JNIEXPORT void JNICALL Java_urine_ahqlab_com_test_MainActivity_SharpenTest(JNIEnv *env, jobject instance, jlong matAddrInput, jlong matAddrResult) {

        Mat &rgb = *(Mat *) matAddrInput;
        //Mat &img_gray = *(Mat *) matGray;
        Mat &img_result = *(Mat *) matAddrResult;

        //cv::Rect rect(Point(460,450), Point((460 + 1000),(450 + 180)));
        //rectangle(rgb, rect, CV_RGB(0, 255, 0), 2);

        //Mat roi = rgb(rect);
        //cvtColor(rgb , roi , COLOR_BGR2GRAY);
        //roi.copyTo(rgb(roi));
        //cv::Rect roi(460, 450, 1460, 630);

        //Imgproc.rectangle(matInput, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0, 255, 0));
        // cv::Mat roiMat = rgb(roi); // you can do image(roi).clone() if you do not want this
        // cv::rectangle(roiMat, rct, CV_RGB(0, 255, 0), 2);
        //Rect area(100, 30, 150, 300);
        //Mat subImage = rgb(area);
        //cvtColor(subImage , subImage , CV_BGR2GRAY);
        //threshold(work , work , 150, 255, THRESH_BINARY);
        // findContours(work,...);
        //cvtColor(work , roi, CV_GRAY2BGR); //here's the trick
        //rgb = roiMat;
        //Mat rgb =  Sharpen(img_input, img_result);


    }
}
