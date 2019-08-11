package com.example.plantdisease;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.plantdisease.Models.RecyclerViewDiseaseAdapter;
import com.example.plantdisease.Models.PlantImage;
import com.example.plantdisease.Models.TensorFlowClassifier;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.synnapps.carouselview.CarouselView;
import com.synnapps.carouselview.ImageClickListener;
import com.synnapps.carouselview.ImageListener;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static androidx.core.content.FileProvider.getUriForFile;


public class MainActivity extends AppCompatActivity {

    // sizes for recognize images
    private static final int RECOGNISE_TYPE_IMAGE_SIZE = 128;

    // Messages
    private static final int IMAGE_CAPTURE_REQUEST = 1001;
    private static final int GALLERY_IMAGE_REQUEST = 1002;

    // ActivityCodes
    private static final int ACTIVITY_IMAGE_DETAILES = 1;
    private static final int ACTIVITY_DISEASE_INFO = 2;

    // General recognize param
    float threshold = 0.0f;
    int showDesiases = 2;

    // Class members
    private TensorFlowClassifier mPlantTypeClassyfier = null;
    private List<Plant> mPlants = new ArrayList();
    public static List<PlantImage> mPomidorImages = new ArrayList<>();
    private boolean firstImageUploaded = false;

    // Controlls
    CarouselView carouselImages;
    int carouselImageCurrent;
    ImageButton btnImageUpload;
    ImageButton btnImageCapture;
    ImageButton btnImageClear;
    ImageButton btnRecognise;
    Button btnTutorial;
    Spinner spinnerPlantCategory;

    TextView textTotalHealth;
    RecyclerView recyclerDiseases;

    // Adapters
    ArrayList<Disease> rvDiseases = new ArrayList<>();
    RecyclerViewDiseaseAdapter rvDiseaseAdapter;


    // Internal
    public static String fileNameImageCapture;

    // Excange
    public static Disease activeDIsease;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // General initialise
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request permission - storage
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
        }

        // Request permission - storage
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1002);
        }

        // Request permission - camera
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1003);
        }

        // Get Controlls Objects
        carouselImages = findViewById(R.id.carouselPomidors);
        btnImageUpload = findViewById(R.id.btnImageUpload);
        btnImageCapture = findViewById(R.id.btnImageCapture);
        btnImageClear = findViewById(R.id.btnImageClear);
        btnRecognise = findViewById(R.id.buttonRecognise);
        spinnerPlantCategory = findViewById(R.id.spinnerPlantCategory);
        textTotalHealth = findViewById(R.id.textTotalHealth);
        recyclerDiseases = findViewById(R.id.recyclerDiseases);
        btnTutorial = findViewById(R.id.btnTutorial);

        // Initialize bitmaps
        mPomidorImages.add(PlantImage.CreatePlantImage(BitmapFactory.decodeResource(getResources(), R.raw.pomidor1), Plant.plant_part_type.fruit));

        // Initialise carousel
        carouselImages.setPageCount(1);
        carouselImages.setImageListener(carouselImageListener);
        carouselImages.setImageClickListener(carouselClickListener);

        // Настрока recycler view
        // set up the RecyclerView
        recyclerDiseases.setLayoutManager(new LinearLayoutManager(this));
        rvDiseaseAdapter = new RecyclerViewDiseaseAdapter(this, rvDiseases);
        rvDiseaseAdapter.setClickListener(new RecyclerViewDiseaseAdapter.ItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                OnDiseaseClick(rvDiseases.get(position));
            }
        });
        recyclerDiseases.setAdapter(rvDiseaseAdapter);

        // Events - select image
        btnImageUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(MainActivity.this);
            }
        });

        // Events - show tutorial
        btnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenTutorial();
            }
        });


        // Events - capture image
        btnImageCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeCameraImage();
            }
        });

        // Events - clear images
        btnImageClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check no images
                if (mPomidorImages.size() == 0)
                    return;

                // Show dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Clear images?")
                        .setMessage("")
                        //.setIcon(R.drawable.)
                        .setCancelable(true)
                        .setNeutralButton("Clear",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // Clear images
                                        mPomidorImages.clear();
                                        carouselImages.setPageCount(0);
                                        spinnerPlantCategory.setSelection(0);
                                        ClearViewContext();
                                    }
                                })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Nothing
                            }
                        });

                // Show alert
                final AlertDialog alert = builder.create();
                alert.show();

                // Update view
                UpdateButtons();
            }
        });


        // Events - recognise
        btnRecognise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DoDiseaseRecognition();
            }
        });

        // Event plant category changed
        spinnerPlantCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View itemSelected, int selectedItemPosition, long selectedId) {

                // Classify image parts again
                if (selectedItemPosition > 0)
                    mPomidorImages.forEach(img -> DoPartClassification(img, true));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // tensorflow
        //load up our saved model to perform inference from local storage
        loadModel();

        // Clear cache
        this.clearCache(this);
    }

    // Clear context
    void ClearViewContext() {
        // Clear diseases
        textTotalHealth.setText("");

        // Update buttons
        UpdateButtons();
    }

    // Update buttons
    void UpdateButtons() {
        btnImageClear.setEnabled(mPomidorImages.size() > 0);
        btnRecognise.setEnabled(mPomidorImages.size() > 0);
    }

    // Load model
    private void loadModel() {
        //The Runnable interface is another way in which you can implement multi-threading other than extending the
        // //Thread class due to the fact that Java allows you to extend only one class. Runnable is just an interface,
        // //which provides the method run.
        // //Threads are implementations and use Runnable to call the method run().
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // load plant type classifyer
                    mPlantTypeClassyfier = TensorFlowClassifier.create(getAssets(),
                            "models/opt_plant_type.pb",
                            "conv2d_1_input", "dense_2/Softmax",
                            RECOGNISE_TYPE_IMAGE_SIZE, 2);

                    // Add Pomidor class
                    mPlants.add(Plant.create(Plant.plant_type.cucumber, RECOGNISE_TYPE_IMAGE_SIZE, 0));
                    mPlants.add(Plant.create(Plant.plant_type.tomato, RECOGNISE_TYPE_IMAGE_SIZE, 18));


                    // Do laod models
                    mPlants.forEach(v -> v.LoadClassificationModel(getAssets()));

                } catch (final Exception e) {
                    //if they aren't found, throw an error!
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }

    // Carousel view managing function
    ImageClickListener carouselClickListener = new ImageClickListener() {
        @Override
        public void onClick(int position) {
            carouselImageCurrent = position;
            Intent intent = new Intent(MainActivity.this, ImageDetailsActivity.class);
            intent.putExtra("image_pos", position);
            startActivityForResult(intent, ACTIVITY_IMAGE_DETAILES);
        }
    };

    // Carousel managing function
    ImageListener carouselImageListener = new ImageListener() {
        @Override
        public void setImageForPosition(int position, ImageView imageView) {
            imageView.setImageBitmap(mPomidorImages.get(position).image);
        }
    };


    // Add image to caourusel
    private void addImageCarousel(Bitmap bmp) {
        // If first image is not loaded - remove decorate image
        if (!firstImageUploaded) {
            mPomidorImages.clear();
            firstImageUploaded = true;
        }

        // Process image
        int bmp_size = Math.max(bmp.getWidth(), bmp.getHeight());
        Bitmap bmp_processed = Bitmap.createBitmap(bmp_size, bmp_size,
                Bitmap.Config.ARGB_8888);
        bmp_processed.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(bmp_processed);
        canvas.drawBitmap(bmp, bmp_processed.getWidth() / 2 - bmp.getWidth() / 2, bmp_processed.getHeight() / 2 - bmp.getHeight() / 2, new Paint());

        // Do add image to array
        PlantImage plti = PlantImage.CreatePlantImage(bmp_processed, Plant.plant_part_type.undefined);
        mPomidorImages.add(plti);
        carouselImages.setPageCount(mPomidorImages.size());
        carouselImages.setCurrentItem(mPomidorImages.size() - 1, true);

        // Do classification
        DoPlantClassification();

        // Update part classification
        DoPartClassification(plti, false);

        // Update view
        UpdateButtons();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
            switch (requestCode) {
                case IMAGE_CAPTURE_REQUEST:
                    cropImage(getCacheImagePath(fileNameImageCapture));
                    break;
                case GALLERY_IMAGE_REQUEST:
                    Uri imageUri = data.getData();
                    cropImage(imageUri);
                    break;
                case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                    CropImage.ActivityResult result = CropImage.getActivityResult(data);
                    Uri resultUri = result.getUri();

                    try {
                        InputStream imageStream = getContentResolver().openInputStream(resultUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                        addImageCarousel(bitmap);
                    } catch (IOException e) {
                        Log.i("TAG", "Some exception " + e);
                        return;
                    }

                    break;
                case ACTIVITY_IMAGE_DETAILES:
                    String action = data.getStringExtra("action");
                    carouselImages.setPageCount(mPomidorImages.size()); // Upadte images
                    switch (action) {
                        case "remove":
                            mPomidorImages.remove(carouselImageCurrent);
                            carouselImages.setPageCount(mPomidorImages.size());
                            if (carouselImageCurrent < mPomidorImages.size())
                                carouselImages.setCurrentItem(carouselImageCurrent, true);
                            else if (mPomidorImages.size() > 0)
                                carouselImages.setCurrentItem(mPomidorImages.size() - 1, true);
                            break;
                    }
                    break;
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If grant results is 0
        if (grantResults.length == 0)
            return;

        // Else do process
        switch (requestCode) {
            case 1001:
            case 1002:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Storage permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Storage permission not granted :(((", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case 1003: { // Camera
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Camera permission not granted :(((", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private void takeCameraImage() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            fileNameImageCapture = System.currentTimeMillis() + ".jpg";
                            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, getCacheImagePath(fileNameImageCapture));
                            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST);
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void chooseImageFromGallery() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, GALLERY_IMAGE_REQUEST);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    // Activate plant image
    void ActivatePlantImage(PlantImage plnti) {
        // TO be developed

    }


    private static String queryName(ContentResolver resolver, Uri uri) {
        Cursor returnCursor =
                resolver.query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    private void cropImage(Uri sourceUri) {
        CropImage.activity(sourceUri)
                .start(this);
    }

    /**
     * Calling this will delete the images from cache directory
     * useful to clear some memory
     */
    public static void clearCache(Context context) {
        // camera images
        File path = new File(context.getExternalCacheDir(), "camera");
        if (path.exists() && path.isDirectory()) {
            for (File child : path.listFiles()) {
                child.delete();
            }
        }
    }

    private Uri getCacheImagePath(String fileName) {
        File path = new File(getExternalCacheDir(), "camera");
        if (!path.exists()) path.mkdirs();
        File image = new File(path, fileName);
        return getUriForFile(this, getPackageName() + ".provider", image);
    }

    protected void DoPlantClassification() {
        // There are images
        if (mPomidorImages.size() == 0)
            return;

        // Check image not classified yet
        if (spinnerPlantCategory.getSelectedItemPosition() != 0)
            return;

        // Do classification
        int plant_type = mPlantTypeClassyfier.recognize(mPomidorImages) + 1; // 1 to store undefined

        // set plant
        if (plant_type != spinnerPlantCategory.getSelectedItemPosition())
            spinnerPlantCategory.setSelection(plant_type);

        // Do disease recognition
        //DoDiseaseRecognition();
    }

    protected void DoPartClassification(PlantImage image, boolean silent) {
        // Check plant type
        int plantId = spinnerPlantCategory.getSelectedItemPosition();
        if (plantId == 0) {
            Toast.makeText(getApplicationContext(), "Unable to recognase plant", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get plant object
        Plant recognisedPlant = mPlants.get(plantId - 1);
        image.plantPart = recognisedPlant.DoPartClassification(image);

        // Show
        if (!silent) {
            String msg = image.plantPart.toString();
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show(); // Remove later
        }
    }

    protected void DoDiseaseRecognition() {
        // Recognise plant again
        DoPlantClassification();

        // Check plant type
        int plantId = spinnerPlantCategory.getSelectedItemPosition();
        if (plantId == 0) {
            Toast.makeText(getApplicationContext(), "Unable to recognise undefined plant", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get plant object
        Plant recognisedPlant = mPlants.get(plantId - 1);
        float[] recognisedDisease = recognisedPlant.DoDiseaseRecognition(mPomidorImages);

        // Check result
        if (recognisedDisease == null || recognisedDisease.length < 1) {
            Toast.makeText(this, R.string.model_not_loaded, Toast.LENGTH_SHORT).show();
            ClearViewContext();
            return;
        }

        // Show total health count
        String health = getString(R.string.health_val) + "    " + Math.round(recognisedDisease[0] * 100) + "%";
        textTotalHealth.setText(health);

        // Set text color
        if (recognisedDisease[0] > 0.67f)
            textTotalHealth.setTextColor(ContextCompat.getColor(this, R.color.colorHealthGood));
        else if (recognisedDisease[0] > 0.33f)
            textTotalHealth.setTextColor(ContextCompat.getColor(this, R.color.colorHealthMed));
        else
            textTotalHealth.setTextColor(ContextCompat.getColor(this, R.color.colorHealthPoor));

        // show diseases
        rvDiseases.clear();
        for (int i = 1; i < recognisedDisease.length; i++)
            if (recognisedDisease[i] > threshold) {
                rvDiseases.add(recognisedPlant.CreateDisease(getAssets(), i, recognisedDisease[i]));
            }

        // Sort diseases and show
        if (rvDiseases.size() > 0) {
            rvDiseases.sort((a, b) -> Double.compare(a.probability, b.probability));
            while (rvDiseases.size() > showDesiases)
                rvDiseases.remove(showDesiases);
        }
        recyclerDiseases.setAdapter(rvDiseaseAdapter);

        // Update view
        UpdateButtons();


        // SendImages home
        SendImagesHome();
    }

    // Disease click
    void OnDiseaseClick(Disease disease) {
        activeDIsease = disease;
        Intent intent = new Intent(MainActivity.this, DiseaseInfoActivity.class);
        startActivityForResult(intent, ACTIVITY_DISEASE_INFO);
    }

    // Open tutorial
    void OpenTutorial() {
        activeDIsease = null;
        Intent intent = new Intent(MainActivity.this, DiseaseInfoActivity.class);
        startActivityForResult(intent, ACTIVITY_DISEASE_INFO);
    }

    // Send images home
    void SendImagesHome() {
        // Do it in parallel
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mPomidorImages.forEach(img -> img.SendHome());
                } catch (final Exception e) {
                }
            }
        }).start();
    }
}
