package com.example.plantdisease;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import com.example.plantdisease.Models.PlantImage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static androidx.core.content.FileProvider.getUriForFile;

public class ImageDetailsActivity extends AppCompatActivity {
    // Data
    PlantImage imageObject;

    // Controlls
    ImageView imagePlant;
    Spinner spinnerPlantPart;
    ImageButton btnCrop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Get data
        imagePlant = findViewById(R.id.imagePlant);
        spinnerPlantPart = findViewById(R.id.spinnerPlantPart);
        btnCrop = findViewById(R.id.btnCrop);


        // Read input data
        Intent intent = getIntent();
        int image_id = intent.getIntExtra("image_pos", -1);
        if (image_id < 0)
            finish();
        imageObject = MainActivity.mPomidorImages.get(image_id);

        // Set initial data
        imagePlant.setImageBitmap(imageObject.image);
        spinnerPlantPart.setSelection(Plant.GetPlantPartCode(imageObject.plantPart) + 1);


        // Add floating buttons
        FloatingActionButton fab = findViewById(R.id.removeImage);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(ImageDetailsActivity.this);
                builder.setTitle("Remove image?")
                        .setMessage("")
                        //.setIcon(R.drawable.)
                        .setCancelable(true)
                        .setNeutralButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // send reult
                                        Intent answerIntent = new Intent();
                                        answerIntent.putExtra("action", "remove");
                                        setResult(RESULT_OK, answerIntent);
                                        finish();
                                    }
                                })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Nothing
                            }
                        });

                // Show alert
                final AlertDialog alert = builder.create();
                alert.show();
            }
        });

        // Event - plant category changed
        spinnerPlantPart.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View itemSelected, int selectedItemPosition, long selectedId) {
                imageObject.plantPart = Plant.GetPlantPart(selectedItemPosition - 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Events - crop
        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // CHeck permissions
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1002);

                // Put image 2 file
                File image;
                try {
                    File path = new File(getExternalCacheDir(), "camera");
                    image = File.createTempFile(System.currentTimeMillis() + "", ".jpg", path);
                    if(!path.exists()) {path.mkdirs();}

                    FileOutputStream fOut = new FileOutputStream(image);
                    imageObject.image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                    fOut.flush();
                    fOut.close();
                } catch (final Exception e) {
                    //if they aren't found, throw an error!
                    throw new RuntimeException("Error image crop!", e);
                }

                // Do crop
                Uri imageUri = getUriForFile(ImageDetailsActivity.this, getPackageName() + ".provider", image);
                CropImage.activity(imageUri)
                        .start(ImageDetailsActivity.this);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
            switch (requestCode) {
                case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
                    Uri resultUri = result.getUri();

                    try {
                        InputStream imageStream = getContentResolver().openInputStream(resultUri);
                        imageObject.image = BitmapFactory.decodeStream(imageStream);
                        imagePlant.setImageBitmap(imageObject.image); // Update image view
                    } catch (IOException e) {
                        Log.i("TAG", "Some exception " + e);
                        return;
                    }

                    // Return action
                    Intent answerIntent = new Intent();
                    answerIntent.putExtra("action", "image_changed");
                    setResult(RESULT_OK, answerIntent);

                    break;
            }
    }
}
