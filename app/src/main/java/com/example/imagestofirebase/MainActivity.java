package com.example.imagestofirebase;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    String Storage_Path = "All_Image_Uploads/";
    public static final String Database_Path = "All_Image_Uploads_Database";
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    Button ChooseButton, UploadButton, DisplayImageButton,cameraBtn;
    EditText ImageName ;
    ImageView SelectImage;
    Uri PathUri;
    StorageReference storageReference;
    DatabaseReference databaseReference;

    ProgressDialog progressDialog ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference(Database_Path);
        UploadButton = (Button)findViewById(R.id.ButtonUploadImage);
        cameraBtn = (Button)findViewById(R.id.cameraBtn);
        DisplayImageButton = (Button)findViewById(R.id.DisplayImagesButton);
        ImageName = (EditText)findViewById(R.id.ImageNameEditText);
        SelectImage = (ImageView)findViewById(R.id.ShowImageView);
        progressDialog = new ProgressDialog(MainActivity.this);
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        UploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Calling method to upload selected image on Firebase storage.
                UploadImageFileToFirebaseStorage();

            }
        });


        DisplayImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, DisplayImagesActivity.class);
                startActivity(intent);

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

         if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {

            PathUri = data.getData();

            Bitmap photo = (Bitmap) data.getExtras().get("data");
             SelectImage.setImageBitmap(photo);

        }
    }


    // Creating UploadImageFileToFirebaseStorage method to upload image on storage.
    public void UploadImageFileToFirebaseStorage() {

        // Checking whether FilePathUri Is empty or not.
        if (PathUri != null) {
            progressDialog.setTitle("Image is Uploading...");
            progressDialog.show();
            String uniqueId = UUID.randomUUID().toString();
            final StorageReference ur_firebase_reference = storageReference.child("user_photos/" + uniqueId);
            UploadTask uploadTask = ur_firebase_reference.putFile(PathUri);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

            return ur_firebase_reference.getDownloadUrl();
        }
    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
        @Override
        public void onComplete(@NonNull Task<Uri> task) {
            if (task.isSuccessful()) {
                progressDialog.dismiss();
                String TempImageName = ImageName.getText().toString().trim();
                Uri downloadUri = task.getResult();
                System.out.println("Upload " + downloadUri);
                if (downloadUri != null) {

                    String ImageUploadId = downloadUri.toString();
                    System.out.println("Upload " + ImageUploadId);
                    // Getting image upload ID.
                    ImageUploadId = databaseReference.push().getKey();
                    ImageUploadInfo imageUploadInfo = new ImageUploadInfo(TempImageName, ImageUploadId);

                    // Getting image upload ID.

                    // Adding image upload id s child element into databaseReference.
                    databaseReference.child(ImageUploadId).setValue(imageUploadInfo);

                }

            } else {
                // Handle failures
                // ...
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this,"plzz capture image", Toast.LENGTH_LONG).show();
            }
        }
    });
        }
        else {

            Toast.makeText(MainActivity.this, "Please Select Image or Add Image Name", Toast.LENGTH_LONG).show();

        }
    }


}