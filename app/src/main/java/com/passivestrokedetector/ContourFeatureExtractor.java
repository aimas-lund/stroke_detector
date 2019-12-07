package com.passivestrokedetector;

import com.google.firebase.ml.vision.face.FirebaseVisionFace;

import java.util.ArrayList;
import java.util.List;

public class ContourFeatureExtractor {

    private FirebaseVisionFace face;

    ContourFeatureExtractor(FirebaseVisionFace face) {
        this.face = face;
    }

    public List<Double> extract() {

        List<Double> output = new ArrayList<>();


        return output;
    }

    private Double averageOf(List<Double> arr) {
        Double sum = 0.0;

        for (int i = 0; i<arr.size(); i++) {
            sum += arr.get(i);
        }
        return sum/arr.size();
    }

    private Double varianceOf(List<Double> arr){
        double x = 0.0;
        Double mean = averageOf(arr);

        for (int i = 0; i<arr.size(); i++) {
            x += Math.pow((arr.get(i) - mean), 2);
        }
        return x/arr.size();
    }

    public FirebaseVisionFace getFace() {
        return face;
    }

    public void setFace(FirebaseVisionFace face) {
        this.face = face;
    }
}
