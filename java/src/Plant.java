package com.example.plantdisease;

import android.content.res.AssetManager;

import com.example.plantdisease.Models.PlantImage;
import com.example.plantdisease.Models.TensorFlowClassifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Plant {
    // Types
    public enum plant_type {undefined, cucumber, tomato};
    public enum plant_part_type {undefined, leaf, fruit, stem};

    // Static
    public static Map<plant_type, Integer> plantCodes = new HashMap<plant_type, Integer>();


    // Members
    public plant_type type = plant_type.undefined;
    TensorFlowClassifier mPlantPartClassifyer = null;
    private Map<plant_part_type, TensorFlowClassifier> mClassifiersDiseaseByParts = new HashMap<plant_part_type, TensorFlowClassifier>();

    // Model params
    int modelImageSize = 128;
    int diseaseCount = 0;

    // Constructor
    public Plant() {
        // Add plant codes
        if (plantCodes.size() == 0) { // Static!
            plantCodes.put(plant_type.cucumber, 0);
            plantCodes.put(plant_type.tomato, 1);
        }

    }


    // Factory
    public static Plant create(plant_type tp, int recognise_image_size, int disCount){
        // Create class
        Plant p = new Plant();

        // Put values
        p.type = tp;
        p.modelImageSize = recognise_image_size;
        p.diseaseCount = disCount;

        // Return
        return p;
    }

    // Get name
    public String getName() {
        return type.toString();
    }

    // Add model
    private TensorFlowClassifier LoadDiseasePartModel(AssetManager assetManager, plant_part_type pt, int layerInNumber, int layerOutNumber) {
        // Prepare strings
        String modelPath = "models/" + getName() + "/opt_disease_cat_4_" + pt.toString() + ".pb";
        String imputName = "conv2d_" + layerInNumber + "_input";
        String outputName =  "dense_" + layerOutNumber + "/Softmax";

        // Create classyfier
        TensorFlowClassifier classif = null;
        try {
            classif = TensorFlowClassifier.create(assetManager,
                modelPath,
                    imputName, outputName,
                modelImageSize, diseaseCount);
        } catch (final Exception e) {
            //if they aren't found, throw an error!
            throw new RuntimeException("Can not load disease classifier " + pt.toString(), e);
        }
        // Return
        return classif;
    }

    // Load models
    public void LoadClassificationModel(AssetManager assetManager) {
        // has diseases
        if (diseaseCount == 0)
            return;

        int layerInNumber = 4; // !!!!!!!!!!!!!!! - for future revork - after OGURETZZZ !!!!s
        int layerOutNumber = 4;

        // Add clasifyer plant part
        try {
            String imputName = "conv2d_" + layerInNumber + "_input";
            String outputName =  "dense_" + layerOutNumber + "/Softmax";
            mPlantPartClassifyer = TensorFlowClassifier.create(assetManager,
                                "models/" + getName() + "/opt_part.pb",
                    imputName, outputName,
                                modelImageSize, 3);
        } catch (final Exception e) {
            //if they aren't found, throw an error!
            throw new RuntimeException("Can not load plant part type classyfier!", e);
        }

        // Add disease classifyers
        mClassifiersDiseaseByParts.put(plant_part_type.leaf, LoadDiseasePartModel(assetManager, plant_part_type.leaf, 1, 2));
        mClassifiersDiseaseByParts.put(plant_part_type.fruit, LoadDiseasePartModel(assetManager, plant_part_type.fruit, 4, 4));
        mClassifiersDiseaseByParts.put(plant_part_type.stem, LoadDiseasePartModel(assetManager, plant_part_type.stem, 7, 6));
    }

    // Get plant type by int
    protected static plant_part_type GetPlantPart(int val) {
        switch (val) {
            case 0:
                return plant_part_type.leaf;
            case 1:
                return plant_part_type.fruit;
            case 2:
                return plant_part_type.stem;
        }

        // Other
        return plant_part_type.undefined;
    }

    // Get plant type by int
    protected static int GetPlantPartCode(plant_part_type val) {
        switch (val) {
            case undefined:
                return -1;
            case leaf:
                return 0;
            case fruit:
                return 1;
            case stem:
                return 2;
        }

        // Other
        return 0;
    }

    // Do classification
    public float[] DoDiseaseRecognition(List<PlantImage> imgList) {
        // has diseases
        if (diseaseCount == 0)
            return null;

        // Create arrays for plant parts
        Map<plant_part_type, List<PlantImage>> imagesByParts = new HashMap<plant_part_type, List<PlantImage>>();
        //imagesByParts.put(plant_part_type.undefined, new ArrayList<>());
        imagesByParts.put(plant_part_type.leaf, new ArrayList<>());
        imagesByParts.put(plant_part_type.fruit, new ArrayList<>());
        imagesByParts.put(plant_part_type.stem, new ArrayList<>());
        for (PlantImage image : imgList) {
            imagesByParts.get(image.plantPart).add(image);
        }

        // Get classifications for all images
        float[] recognisedDissTotalScore = new float[diseaseCount];
        for (Map.Entry<plant_part_type, List<PlantImage>> images: imagesByParts.entrySet()) {
            if (images.getValue().size() > 0) {
                // Recognise image set
                float[] dissByPart = mClassifiersDiseaseByParts.get(images.getKey()).recognize_vector(images.getValue(), false);
                for(int i=0; i< diseaseCount; i++)
                    recognisedDissTotalScore[i] += dissByPart[i];
            }
        }

        // Normalise res by images
        for(int i=0; i< diseaseCount; i++)
            recognisedDissTotalScore[i] /= imgList.size();

        // Get class
        /*float maxScoree = 0;
        int disease = -1;
        for(int i=0; i< diseaseCount; i++)
            if (recognisedDissTotalScore[i] > maxScoree) {
                maxScoree = recognisedDissTotalScore[i];
                disease = i;
            }*/

        // Return
        return recognisedDissTotalScore;
    }

    // Recognise part of the plant
    public plant_part_type DoPartClassification(PlantImage image){
        if (mPlantPartClassifyer != null)
            return GetPlantPart(mPlantPartClassifyer.recognize(image));
        else
            return plant_part_type.undefined;
    }

    // Get dis discription
    public Disease CreateDisease(AssetManager am, int disease_id, float prob) {
        Disease disease = new Disease(prob, this);
        disease.probability = prob;
        String diseaseFileName = "diseases/" + getName() + "/" + disease_id + ".txt";

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(am.open(diseaseFileName)));
            disease.name = br.readLine();
            br.close();
        } catch (final Exception e) {
            //if they aren't found, throw an error!
            throw new RuntimeException("Can not load plant name", e);
        }

        return disease;
    }

    // Get plant type
    static public plant_type GetPlantType(int pt) {
        // Lop all values
        for (Map.Entry<plant_type, Integer> pt_val  : plantCodes.entrySet()) {
            if (pt_val.getValue() == pt)
                return pt_val.getKey();
        }

        // Not found
        return plant_type.undefined;
    }
}
