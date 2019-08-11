package com.example.plantdisease;

import android.content.res.AssetManager;

public class Disease {
    // Data
    public String name = "";
    public float probability = 1;
    public Plant parentPlant;

    // Constructor
    public Disease(float prob, Plant parent) {
        probability = prob;
        parentPlant = parent;
    }
}
