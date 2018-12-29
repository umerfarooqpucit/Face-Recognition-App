package com.example.umer.smartgallery;
import org.apache.commons.io.IOUtils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_core.*;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.Function;
import org.bytedeco.javacpp.opencv_objdetect;

import static org.bytedeco.javacpp.opencv_imgproc.*;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;


import android.text.format.Time;

import com.google.android.gms.auth.api.signin.internal.Storage;

import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.opencv.android.Utils.matToBitmap;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.opencv.core.MatOfInt;

public class MainActivity extends AppCompatActivity  {

    private ImageView image = null;
    private Button trainBtn=null;
    private Button recognizeBtn=null;
    private TextView txt=null;
    private EditText etPerson=null;
    private Button addPersonNameBtn=null;

    private CvHaarClassifierCascade classifier = null;
    private CvHaarClassifierCascade classifierSideFace = null;
    private CvMemStorage storage = null;
    private CvSeq faces = null;
    private File classifierFile;
    Recognizer recognizer;
    private File tempFile = null;

    MatVector matVector;
    Mat label;

    private SharedPreferences labelName;

    private String personName;
    private Uri mHighQualityImageUri = null;

    private final int REQUEST_CODE_RECOGNIZE = 1;
    private final int REQUEST_CODE_TRAIN = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        File myMainDir =new File(getApplicationContext().getFilesDir(),"faceRecognizer");
        if(!myMainDir.exists()){
            myMainDir.mkdirs();
        }
        File facesDir = new File(myMainDir,"faces");
        if(!facesDir.exists()){
            facesDir.mkdirs();
        }
        File haarDir = new File(myMainDir,"haar");
        if(!haarDir.exists()){
            haarDir.mkdirs();
        }
        recognizer=new Recognizer();
        recognizer.setFaceRecognizer(getApplicationContext());
        //recognizer.init(getApplicationContext());

        if(savedInstanceState!=null){
            //recognizer=(Recognizer)savedInstanceState.getSerializable("recognizerInstance");
        }

        InputStream input= getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
        classifierFile=inputStreamToFile(input);
        classifier = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        input=getResources().openRawResource(R.raw.haarcascade_profileface);
        classifierFile=inputStreamToFile(input);
        classifierSideFace = new CvHaarClassifierCascade(cvLoad(classifierFile.getAbsolutePath()));
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        storage = CvMemStorage.create();
        classifierFile.delete();


        //classifierFile= new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/faceRecognizer/haar", "haarcascade_frontalface_alt.xml");

        //setClassifier("haarcascade_frontalface_alt.xml");

        setContentView(R.layout.activity_main);
        image= findViewById(R.id.imgView);
        trainBtn= findViewById(R.id.btn_add_train_face);
        recognizeBtn= findViewById(R.id.btn_recognize);
        txt= findViewById(R.id.txt);
        etPerson=findViewById(R.id.etv_person_name);
        addPersonNameBtn=findViewById(R.id.btn_add_person_name);
        labelName=getSharedPreferences("labelName1", Context.MODE_PRIVATE);

        trainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //MessageDialog dialog = new MessageDialog(MainActivity.this);
                //dialog.addNewMessage(R.layout.dialog_layout);
                //String personName= dialog.getEnteredText();
                etPerson.setVisibility(View.VISIBLE);
                addPersonNameBtn.setVisibility(View.VISIBLE);

            }
        });
        addPersonNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                personName=etPerson.getText().toString();
                personName.toLowerCase();
                int count=labelName.getInt("count",0);


                SharedPreferences.Editor editor=labelName.edit();
                if(labelName.getInt(personName,0)==0){
                    count++;
                    editor.putInt("count",count);
                    editor.putInt(personName,count);
                    editor.putString(String.valueOf(count),personName);
                }
                editor.commit();
                trainNewFace();
            }
        });

        recognizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizeFace();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putSerializable("recognizerInstance",recognizer);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //recognizer=(Recognizer)savedInstanceState.getSerializable("recognizerInstance");
    }

    @Override
    protected void onRestart(){
        super.onRestart();
    }
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        File model=new File(getApplicationContext().getFilesDir().getAbsolutePath()+"/faceRecognizer/haar", "model.xml");
        recognizer.save(model.getAbsolutePath());
        super.onDestroy();
    }

    public void recognizeFace() {
        try {
            tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath(),  "temp.jpg");
            //tempFile.deleteOnExit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mHighQualityImageUri=FileProvider.getUriForFile(MainActivity.this, "com.example.android.fileprovider5", tempFile);//Uri.fromFile(tempFile);//Uri.fromFile(tempFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mHighQualityImageUri);
        startActivityForResult(intent, REQUEST_CODE_RECOGNIZE);
    }

    public File inputStreamToFile(InputStream input){
        File file = new File(getCacheDir(), "cascade.xml");
        try{

            OutputStream output = new FileOutputStream(file);
            try{
                byte[] buffer = new byte[2* 1024]; // or other buffer size
                int read;

                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            }finally{
                output.close();
            }

        }catch (FileNotFoundException e) {
                e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    // Button click listener
    public void trainNewFace() {

        //File directory = new File(Environment.getExternalStorageDirectory().getPath()+"/faceRecognizer/faces"+File.separator+personName);
        File directory = new File(getApplicationContext().getFilesDir().getAbsolutePath()+"/faceRecognizer/faces"+File.separator+personName);
        //boolean success = true;
        if (!directory.exists()) {
            directory.mkdirs();
        }

        //File file = new File(Environment.getExternalStorageDirectory().getPath()+"/faceRecognizer/faces",  (personName+".jpg"));
        //File file = new File(Environment.getExternalStorageDirectory().getPath(),  "temp.jpg");

        try {
            tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath()+ "/temp.jpg");
            //tempFile.deleteOnExit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mHighQualityImageUri = generateTimeStampPhotoFileUri();
        mHighQualityImageUri= FileProvider.getUriForFile(MainActivity.this, "com.example.android.fileprovider5", tempFile);//Uri.fromFile(tempFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mHighQualityImageUri);
        startActivityForResult(intent, REQUEST_CODE_TRAIN);


    }

    private Uri generateTimeStampPhotoFileUri() {

        Uri photoFileUri = null;
        File outputDir = getPhotoDirectory();
        if (outputDir != null) {
            Time t = new Time();
            t.setToNow();
            File photoFile = new File(outputDir, System.currentTimeMillis()
                    + ".jpg");
            photoFileUri = Uri.fromFile(photoFile);
        }
        return photoFileUri;

    }

    private File getPhotoDirectory() {
        File outputDir = null;
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            File photoDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            outputDir = new File(photoDir, getString(R.string.app_name));
            if (!outputDir.exists())
                if (!outputDir.mkdirs()) {
                    Toast.makeText(
                            this,
                            "Failed to create directory "
                                    + outputDir.getAbsolutePath(),
                            Toast.LENGTH_SHORT).show();
                    outputDir = null;
                }
        }
        return outputDir;
    }

    public IplImage BitmapToIplImage(Bitmap bmp) {


        IplImage image = IplImage.create(bmp.getWidth(), bmp.getHeight(),
                IPL_DEPTH_8U, 4);

        bmp.copyPixelsToBuffer(image.getByteBuffer());

        IplImage grayImg = IplImage.create(image.width(), image.height(),
                IPL_DEPTH_8U, 1);

        cvCvtColor(image, grayImg, opencv_imgproc.CV_BGR2GRAY);

        return grayImg;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        etPerson.setVisibility(View.GONE);
        addPersonNameBtn.setVisibility(View.GONE);

        if (resultCode == RESULT_OK) {
            if (requestCode==REQUEST_CODE_RECOGNIZE) {

                Bitmap capturedBmp = null;
                try {
                    capturedBmp = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), mHighQualityImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                tempFile.delete();

                Bitmap scaledBmp = recognizer.scaleDown(capturedBmp, 480, true);
                IplImage trainFace = BitmapToIplImage(scaledBmp);


                faces = cvHaarDetectObjects(trainFace, classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
                //storage.release();
                if(faces.total()==0){
                    faces = cvHaarDetectObjects(trainFace, classifierSideFace, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
                }
                for (int i = 0; i < faces.total(); i++) {
                    CvRect r = new CvRect(cvGetSeqElem(faces, i));
                    cvSetImageROI(trainFace, r);

                    IplImage cropped = cvCreateImage(cvGetSize(trainFace), trainFace.depth(), trainFace.nChannels());
                    // Copy original image (only ROI) to the cropped image
                    cvCopy(trainFace, cropped);

                    IplImage resizedGrayImage=IplImage.create(185, 185,
                            IPL_DEPTH_8U, 1);
                    cvResize(cropped,resizedGrayImage);

                    int predictedLabel=recognizer.recognize(resizedGrayImage);
                    if(predictedLabel==-1){
                        Toast.makeText(getApplicationContext(),"Unknown Person",Toast.LENGTH_LONG).show();
                    }else{
                        String predictedPerson=labelName.getString(String.valueOf(predictedLabel),"");
                        Toast.makeText(getApplicationContext(),"The person looks like "+predictedPerson,Toast.LENGTH_LONG).show();
                    }

                    //cvReleaseImage(cropped);
                    //resizedImage.release();
                }
                //cvReleaseImage(trainFace);
            }
            else if(requestCode==REQUEST_CODE_TRAIN) {

                Bitmap myBitmap = null;
                try {
                    myBitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), mHighQualityImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                tempFile.delete();
                Bitmap myBitmap1 = recognizer.scaleDown(myBitmap, 480, true);

                IplImage trainFace = BitmapToIplImage(myBitmap1);



                faces = cvHaarDetectObjects(trainFace, classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
                //storage.release();
                if(faces.total()==0){
                    faces = cvHaarDetectObjects(trainFace, classifierSideFace, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
                }
                for (int i = 0; i < faces.total(); i++) {//Drawing rectangle on all faces found
                    //Face r = faces.valueAt(i);
                    CvRect r = new CvRect(cvGetSeqElem(faces, i));

                    cvSetImageROI(trainFace, r);

                    IplImage cropped = cvCreateImage(cvGetSize(trainFace), trainFace.depth(), trainFace.nChannels());
                    // Copy original image (only ROI) to the cropped image
                    cvCopy(trainFace, cropped);

                    IplImage resizedGrayImage=IplImage.create(185, 185,
                            IPL_DEPTH_8U, 1);
                    cvResize(cropped,resizedGrayImage);

                    Mat resizedImage = cvarrToMat(resizedGrayImage);

                    //File croppedFace = new File(Environment.getExternalStorageDirectory().getPath() + "/faceRecognizer/faces/" + personName, System.currentTimeMillis() + ".jpg");
                    File croppedFace = new File(getApplicationContext().getFilesDir().getPath() + "/faceRecognizer/faces/" + personName, System.currentTimeMillis() + ".jpg");

                    imwrite(croppedFace.getAbsolutePath(), resizedImage);

                    matVector = new MatVector(1);
                    matVector.put(0,resizedImage);

                    label = new Mat(1, 1, CV_32SC1);
                    IntBuffer labelsBuf = label.createBuffer();
                    labelsBuf.put(0, labelName.getInt(personName, 0));

                    recognizer.update(matVector, label);

                    Toast.makeText(getApplicationContext(),"Face is saved",Toast.LENGTH_LONG).show();
                }
                //cvReleaseImage(trainFace);

            }
        }
    }
}

