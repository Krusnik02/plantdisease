package com.example.plantdisease.Models;


//Provides access to an application's raw asset files;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
//Reads text from a character-input stream, buffering characters so as to provide for the efficient reading of characters, arrays, and lines.
import java.io.BufferedReader;
//for erros
import java.io.IOException;
//An InputStreamReader is a bridge from byte streams to character streams:
// //It reads bytes and decodes them into characters using a specified charset.
// //The charset that it uses may be specified by name or may be given explicitly, or the platform's default charset may be accepted.
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
//made by google, used as the window between android and tensorflow native C++
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

/**
 * Changed from https://github.com/MindorksOpenSource/AndroidTensorFlowMNISTExample/blob/master
 * /app/src/main/java/com/mindorks/tensorflowexample/TensorFlowImageClassifier.java
 * Created by marianne-linhares on 20/04/17.
 */

//lets create this classifer
public class TensorFlowClassifier {

    // General paramethers
    private int imageSize = 32;
    private int numClasses = 1;

    // Only returns if at least this confidence
    //must be a classification percetnage greater than this
    private static final float THRESHOLD = 0.5f;

    // TF object
    private TensorFlowInferenceInterface tfHelper;

    // Model params
    private String inputName;
    private String outputName;
    private String[] outputNames;

    // Recognise results
    List<float[]> recognise_res = new ArrayList<>();
    float[] recogz_final = new float[numClasses];


    //given a saved drawn model, lets read all the classification labels that are
    //stored and write them to our in memory labels list
    private static List<String> readLabels(AssetManager am, String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }

        br.close();
        return labels;
    }

   //given a model, its label file, and its metadata
    //fill out a classifier object with all the necessary
    //metadata including output prediction
    public static TensorFlowClassifier create(AssetManager assetManager,
            String modelPath, String inputName, String outputName,
            int imgSize, int numClasses) throws IOException {
        //intialize a classifier
        TensorFlowClassifier c = new TensorFlowClassifier();

        //store its name, input and output labels
        c.imageSize = imgSize;
        c.numClasses = numClasses;
        c.inputName = inputName;
        c.outputName = outputName;
        c.outputNames = new String[] { outputName };

        //set its model path and where the raw asset files are
        c.tfHelper = new TensorFlowInferenceInterface(assetManager, modelPath);

        return c;
    }

    // Normalise bitmep before recognition
    private float[] normalizeBitmap(Bitmap source,int size,float mean,float std){
        // Rescale bitmap
        Bitmap bmp_scaled = Bitmap.createScaledBitmap(source, size, size, true);

        // Do get normilised values
        float[] output = new float[size * size * 3];
        int[] intValues = new int[size * size];
        bmp_scaled.getPixels(intValues, 0, size, 0, 0, size, size);
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            output[i * 3] = (((val >> 16) & 0xFF) - mean)/std;
            output[i * 3 + 1] = (((val >> 8) & 0xFF) - mean)/std;
            output[i * 3 + 2] = ((val & 0xFF) - mean)/std;
        }
        return output;
    }

    // Do recognition of one image
    protected float[] RecognizeImage(Bitmap bmp)
    {
        // Get normalised image
        float[] pixels = normalizeBitmap(bmp, imageSize, 0, 255);
        float[] output = new float[numClasses];

        //using the interface
        //give it the input name, raw pixels from the drawing,
        //input size
        tfHelper.feed(inputName, pixels, 1, imageSize, imageSize, 3);

        //probabilities
        /*if (feedKeepProb) {
            tfHelper.feed("keep_prob", new float[] { 1 });
        }*/
        //get the possible outputs
        tfHelper.run(outputNames);

        //get the output
        tfHelper.fetch(outputName, output);


        // Returm
        return output;
    }

    // Get class by results vector
    private int GetClass(float[] recogz)
    {
        // Find the best classification
        float classMaxVal = 0;
        int classBest = -1;
        for (int i = 0; i < recogz.length; ++i) {
            if (recogz[i] > classMaxVal) {
                classBest = i;
                classMaxVal = recogz[i];
            }
        }
        return classBest;
    }

    // Do recognise one image
    public int recognize(PlantImage image) {
        return GetClass(RecognizeImage(image.image));
    }

    // Do recognition image array
    public int recognize(List<PlantImage> images) {

        // Get recognition results for all images
        recognise_res = new ArrayList<>();
        for (int i=0; i< images.size(); i++)
            recognise_res.add(RecognizeImage(images.get(i).image));

        // Create summary
        recogz_final = new float[numClasses];
        for (int i =0; i < recognise_res.size(); i++) {
            float[] recogs_by_img = recognise_res.get(i);
            for (int cls_id=0; cls_id < numClasses; cls_id++)
                recogz_final[cls_id] += recogs_by_img[cls_id];
        }

        // Normalise
        for (int cls_id=0; cls_id < recogz_final.length; cls_id++)
            recogz_final[cls_id] /= images.size();

        // Return best classification
        return GetClass(recogz_final);
    }


    // Do recognition image array
    public float[] recognize_vector(List<PlantImage> images, boolean normalise) {

        // Get recognition results for all images
        recognise_res = new ArrayList<>();
        for (int i=0; i< images.size(); i++)
            recognise_res.add(RecognizeImage(images.get(i).image));

        // Create summary
        recogz_final = new float[numClasses];
        for (int i =0; i < recognise_res.size(); i++) {
            float[] recogs_by_img = recognise_res.get(i);
            for (int cls_id=0; cls_id < numClasses; cls_id++)
                recogz_final[cls_id] += recogs_by_img[cls_id];
        }

        // Normalise
        if (normalise)
            for (int cls_id=0; cls_id < recogz_final.length; cls_id++)
                recogz_final[cls_id] /= images.size();

        // Return best classification
        return recogz_final;
    }
}
