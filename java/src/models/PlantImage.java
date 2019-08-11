package com.example.plantdisease.Models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.plantdisease.Plant;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;


public class PlantImage {

    // Properties
    public Bitmap image;
    public Plant.plant_part_type plantPart = Plant.plant_part_type.undefined;

    // Send image
    boolean sendedToServer = false;

    // Fucktory
    public static PlantImage CreatePlantImage(Bitmap img, Plant.plant_part_type plntPrt) {
        // Create
        PlantImage p = new PlantImage();
        p.image = img;
        p.plantPart = plntPrt;

        // Return
        return p;
    }

    // Send image to server
    public void SendHome() {
        // Check already sended
        if (sendedToServer)
            return;

        // Do send image

        // TBD



        // Image sended
        sendedToServer = true;
    }
}
