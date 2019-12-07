package com.passivestrokedetector;

import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/*
Face extractor calculates the following attributes:
minX, maxX, sdX, avgX, minY, maxY, sdY, avgY, cSlope
 */
public class ContourFeatureExtractor {

    private FirebaseVisionFace face;

    private List<Double> leftEyeFeatures = new ArrayList<>();
    private List<Double> rightEyeFeatures = new ArrayList<>();
    private List<Double> lowerLipFeatures = new ArrayList<>();
    private List<Double> upperLipFeatures = new ArrayList<>();

    ContourFeatureExtractor(FirebaseVisionFace face) {
        this.face = face;
    }

    public List<Double> extractAll() {
        List<Double> leftEyeFeatures = extract(face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints(),
                FacialFeature.LEFT_EYE);
        List<Double> rightEyeFeatures = extract(face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints(),
                FacialFeature.RIGHT_EYE);
        List<Double> lowerLipFeatures = extract(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints(),
                FacialFeature.LOWER_LIP);
        List<Double> upperLipFeatures = extract(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints(),
                FacialFeature.UPPER_LIP);

        return flattenList(
                Arrays.asList(leftEyeFeatures, rightEyeFeatures, lowerLipFeatures, upperLipFeatures)
        );
    }

    private List<Double> extract(List<FirebaseVisionPoint> contours,
                                FacialFeature facialFeature) {

        List<Double> pointsX = new ArrayList<>();
        List<Double> pointsY = new ArrayList<>();
        List<Double> output = new ArrayList<>();

        for (FirebaseVisionPoint p : contours) {
            pointsX.add(p.getX().doubleValue());
            pointsY.add(p.getY().doubleValue());
        }

        output.add(Collections.min(pointsX));
        output.add(Collections.max(pointsX));
        output.add(varianceOf(pointsX));
        output.add(averageOf(pointsX));
        output.add(Collections.min(pointsY));
        output.add(Collections.max(pointsY));
        output.add(varianceOf(pointsY));
        output.add(averageOf(pointsY));
        output.add(slopeOf(contours, facialFeature));

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

    private Double slopeOf(List<FirebaseVisionPoint> points, FacialFeature facialFeature) {

        int i1 = 0;
        int i2 = 0;

        switch (facialFeature) {
            case LEFT_EYE:
                i2 = 8;
            case RIGHT_EYE:
                i2 = 8;
            case LOWER_LIP:
                i1 = 8;
            case UPPER_LIP:
                i2 = 10;
        }

        FirebaseVisionPoint point1 = points.get(i1);
        FirebaseVisionPoint point2 = points.get(i2);

        return (double) (point2.getY()-point1.getY()) / (point2.getX()-point1.getX());
    }

    public <T> List<T> flattenList(List<List<T>> nested) {
        List<T> output = new ArrayList<>();
        nested.forEach(output::addAll);
        return output;
    }

    public FirebaseVisionFace getFace() {
        return face;
    }

    public void setFace(FirebaseVisionFace face) {
        this.face = face;
    }
}
