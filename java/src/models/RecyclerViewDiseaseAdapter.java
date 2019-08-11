package com.example.plantdisease.Models;

import android.content.Context;
import android.graphics.ColorSpace;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantdisease.Disease;
import com.example.plantdisease.R;

import java.util.ArrayList;
import java.util.List;


public class RecyclerViewDiseaseAdapter extends RecyclerView.Adapter<RecyclerViewDiseaseAdapter.ViewHolder> {
    private List<Disease> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public RecyclerViewDiseaseAdapter(Context context, List<Disease> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_disease_item, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Disease disease = mData.get(position);
        holder.textDiseaseName.setText(HtmlCompat.fromHtml("<u>" + disease.name + "</u>", HtmlCompat.FROM_HTML_MODE_LEGACY));
        holder.textDiseaseProbability.setText("" + Math.round(disease.probability * 100) + "%" );
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textDiseaseName;
        TextView textDiseaseProbability;

        ViewHolder(View itemView) {
            super(itemView);
            textDiseaseName = itemView.findViewById(R.id.textDiseaseName);
            textDiseaseProbability = itemView.findViewById(R.id.textDiseaseProbability);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    Disease getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
