package com.example.facedetectionapp;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button cam;
    ImageView imageView;

    TextView textView;
    Dialog dialog;

    private final static int REQUEST_IMAGE_CAPTURE=124;


    InputImage inputImage;

FaceDetector  faceDetector;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        cam=findViewById(R.id.cam);
        imageView=findViewById(R.id.image);
        textView=findViewById(R.id.text1);

//        dialog=new Dialog(this);
//        View view= LayoutInflater.from(this).inflate(R.layout.fragment_result_dialog,null,false);
//        dialog.setContentView(view);
//        dialog.show();

        FirebaseApp.initializeApp(this);


        cam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFile();
            }
        });
        Toast.makeText(this, "App is started", Toast.LENGTH_SHORT).show();





    }

    private void openFile() {
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(intent,REQUEST_IMAGE_CAPTURE);

        }else{
            Toast.makeText(this, "Failed ...", Toast.LENGTH_SHORT).show();
        }




    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bundle bundle=data.getExtras();

        Bitmap bitmap=(Bitmap) bundle.get("data");


        FaceDetectionProcess(bitmap);
        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();




    }

    public void FaceDetectionProcess(Bitmap bitmap) {


        textView.setText("Face detector in processing...");

        final StringBuilder sb=new StringBuilder();

        BitmapDrawable drawable=(BitmapDrawable) imageView.getDrawable();
        InputImage image=InputImage.fromBitmap(bitmap,0);



        FaceDetectorOptions highAccuracyOpt= new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();

        FaceDetector detector= FaceDetection.getClient(highAccuracyOpt);

        Task<List<Face>> result=detector.process(image);


        result.addOnSuccessListener(new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {

                if(faces.size()!=0){
                    if(faces.size()==1){
                        sb.append(faces.size()+" Face Detected\n\n");
                    }else if(faces.size()>1){
                        sb.append(faces.size()+" Faces Detected\n\n");

                    }

                }
                for(Face face:faces) {
                    //titling and rotating probability
                    int id = face.getTrackingId();
                    float rotY = face.getHeadEulerAngleY();
                    float rotZ = face.getHeadEulerAngleZ();
                    sb.append("1. Face Tracking ID [" + id + "]\n");
                    sb.append("2. Head Rotation to right [" + String.format("%.2f", rotY) + " deg.]\n");
                    sb.append("3. Head Titled Sideways [" + String.format("%.2f", rotZ) + " deg.]\n");
                    //Smiling
                    if(face.getSmilingProbability()>0){
                    float SmilingProbability=face.getSmilingProbability();
                    sb.append("4.Smiling Probability ["+String.format("%.2f",SmilingProbability)+"]\n");
                    }
                    //left eye open probability
                    if(face.getLeftEyeOpenProbability()>0){
                        float leftEyeOpenProbability=face.getLeftEyeOpenProbability();
                        sb.append("5.Left eye open Probability ["+String.format("%.2f",leftEyeOpenProbability)+"]\n");
                    }
                    if(face.getRightEyeOpenProbability()>0){
                        float rightEyeOpenProbability=face.getLeftEyeOpenProbability();
                        sb.append("6.right eye open Probability ["+String.format("%.2f",rightEyeOpenProbability)+"]\n");
                    }
                    sb.append("\n");

                }

                ShowDetection("Face Detection",sb,true);



            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                StringBuilder builder=new StringBuilder();
                builder.append("Sorry ! error");
                ShowDetection("Face Detection",builder,false);
            }
        });


    }

    private void ShowDetection(String faceDetection, StringBuilder sb, boolean b) {
        if(b){
            cam.setVisibility(Button.INVISIBLE);
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());
        if(sb.length()!=0){
            textView.append(sb);
            if(faceDetection.substring(0,faceDetection.indexOf(' ')).equals("OCR")){
                textView.append("\n(Hold the text to copy it!)");

            }else{
                textView.append("(Hold the text to Copy it !)");
            }
            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardManager  clipboard=(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clipData=ClipData.newPlainText(faceDetection,sb);
                    clipboard.setPrimaryClip(clipData);
                    return true;
                }
            });

        }else{
            textView.append(faceDetection.substring(0,faceDetection.indexOf(' '))+"Failed to find anything");
        }
        }else if(!b){
            textView.setText(null);
            textView.setMovementMethod(new ScrollingMovementMethod());
            textView.append(sb);

        }



    }
}