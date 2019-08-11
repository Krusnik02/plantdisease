package com.example.plantdisease;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class DiseaseInfoActivity extends AppCompatActivity {

    // Variables
    Disease disease;

    // Controlls
    WebView wvDiseaseInfo;

    // OnCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_info);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get controlls
        wvDiseaseInfo =  findViewById(R.id.wvDiseaseInfo);

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Read data
        disease = MainActivity.activeDIsease;
        if (disease == null)
            ShowTutorial();
        else {
            toolbar.setTitle(disease.name);
            ShowDisease();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    // Show tutorial
    void ShowTutorial() {
        wvDiseaseInfo.loadUrl(getLocaleHtml("file:///android_asset/tutorials/index"));
    }

    // Show tutorial
    void ShowDisease() {
        String diseaseAddress = getLocaleHtml("file:///android_asset/diseases/" + disease.parentPlant.getName() + "/" + disease.name.toLowerCase());
        wvDiseaseInfo.loadUrl(diseaseAddress);
    }

    String getLocaleHtml(String address) {
        // Get locale
        Locale current = getResources().getConfiguration().getLocales().get(0);

        // Check file exists for current locale
        String urlAddress = address + "-" + current.getLanguage() + ".html";
        if (!exists(urlAddress))
            urlAddress = address + "-" + "en" + ".html";

        // Return
        return urlAddress;
    }

    public static boolean exists(String URLName){
        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //HttpURLConnection.setInstanceFollowRedirects(false)

            HttpURLConnection con =  (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
