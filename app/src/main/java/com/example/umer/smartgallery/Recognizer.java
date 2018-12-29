package com.example.umer.smartgallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.LBPHFaceRecognizer;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.cvarrToMat;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.opencv.core.Core.*;

public class Recognizer {
    private LBPHFaceRecognizer faceRecognizer;
    private File root;
    private MatVector images;
    private Mat labels;
    private SharedPreferences labelName;

//    static{
//        System.loadLibrary("opencv_java");
//    }

    public void setFaceRecognizer(Context context){
        this.faceRecognizer= LBPHFaceRecognizer.create();

        //File model=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/faceRecognizer/haar", "model.xml");
        File model=new File(context.getFilesDir().getAbsolutePath()+"/faceRecognizer/haar", "model.xml");
        if(model.exists()) {
            //this.faceRecognizer = load(model.getAbsolutePath());
            this.faceRecognizer.read(model.getAbsolutePath());
        }else{
            init(context);
        }

    }
    private void init(Context context) {

        //2,8,8,8,200

        // mention the directory the faces has been saved
        //String trainingDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/faceRecognizer/faces";
        String trainingDir = context.getFilesDir().getAbsolutePath() + "/faceRecognizer/faces";
        //String trainingDir = (new File(new File(Environment.getExternalStorageDirectory(), "faceRecognizer"), "faces")).getAbsolutePath();
        Log.i("FaceRecognizer", "init:Path: "+trainingDir);

        labelName=context.getSharedPreferences("labelName1",Context.MODE_PRIVATE);

        //root = new File(trainingDir);
        root = new File(trainingDir);
        File[] files = root.listFiles();
        int folderNo=0;
        if(files!=null){
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    // is directory
                    FilenameFilter imgFilter = new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            name = name.toLowerCase();
                            return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
                        }
                    };
                    File[] imageFiles = inFile.listFiles(imgFilter);

                    this.images = new MatVector(imageFiles.length);
                    this.labels = new Mat(imageFiles.length, 1, CV_32SC1);
                    IntBuffer labelsBuf = labels.createBuffer();


                    int counter = 0;
                    // reading face images from the folder
                    for (File image : imageFiles) {
                        Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);

                        int label = labelName.getInt(inFile.getName(),0);
                        images.put(counter, img);

                        labelsBuf.put(counter, label);
                        counter++;
                    }
                    if(folderNo==0){
                        train(images, labels);
                    }else{
                        update(images, labels);
                    }
                    folderNo++;
                    images.deallocate();
                    labels.deallocate();
                }
            }
        }

    }
    public static LBPHFaceRecognizer load(String filepath)
    {
        LBPHFaceRecognizer retVal = new LBPHFaceRecognizer(load(filepath));

        return retVal;
    }
    public void save(String filepath){
        this.faceRecognizer.save(filepath);
    }
    public void train(MatVector images, Mat labels){
        this.faceRecognizer.train(images, labels);
    }

    public void update(MatVector images, Mat labels){
        this.faceRecognizer.update(images, labels);
    }

    public int recognize(IplImage faceData  ) {

        Mat faces = cvarrToMat(faceData);

        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);
        this.faceRecognizer.predict(faces, label, confidence);

//        if(confidence.get(0)>60){
//            return -1;
//        }
        int predictedLabel = label.get(0);
        return predictedLabel;
    }
    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        if(ratio>=1.0)
            return realImage;
        return newBitmap;
    }
}

