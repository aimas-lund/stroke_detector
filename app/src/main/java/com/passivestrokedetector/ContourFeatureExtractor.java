package com.passivestrokedetector;

import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
Face extractor calculates the following attributes:
minX, maxX, sdX, avgX, minY, maxY, sdY, avgY, cSlope
 */
public class ContourFeatureExtractor {

    private FirebaseVisionFace face;

    ContourFeatureExtractor(FirebaseVisionFace face) {
        this.face = face;
    }

    ContourFeatureExtractor() {
        this.face = null;
    }

    List<Double> extractAll() {
//        List<Double> leftEyeFeatures = extract(face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints(),
//                FacialFeature.LEFT_EYE);
//        List<Double> rightEyeFeatures = extract(face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints(),
//                FacialFeature.RIGHT_EYE);
//        List<Double> lowerLipFeatures = extract(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints(),
//                FacialFeature.LOWER_LIP_BOT);
//
//        List<Double> lowerLipTop = extract(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints(),
//                FacialFeature.LOWER_LIP_TOP );
//        List<Double> upperLipBot = extract(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints(),
//                FacialFeature.UPPER_LIP_BOT);
//        List<Double> upperLipFeatures = extract(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints(),
//                FacialFeature.UPPER_LIP_TOP);
//        return flattenList(
//                Arrays.asList(leftEyeFeatures, rightEyeFeatures, lowerLipFeatures, upperLipFeatures, lowerLipTop, upperLipBot)
//        );
//        List<Double> leftEB = extractContour(face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM).getPoints());
//        List<Double> leftET = extractContour(face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).getPoints());
//        List<Double> rightEB = extractContour(face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM).getPoints());
//        List<Double> rightET = extractContour(face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP).getPoints());
//        List<Double> leftEye = extractContour(face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints());
//        List<Double> rightEye = extractContour(face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints());
        List<Double> upperlt = extractContour(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints());
        List<Double> upperlb = extractContour(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints());
        List<Double> lowerlt = extractContour(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints());
        List<Double> lowerlb = extractContour(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints());

        return flattenList(
//                Arrays.asList(leftEB, leftET, rightEB, rightET, leftEye, rightEye, upperlt, upperlb, lowerlt, lowerlb)
                Arrays.asList(upperlt, upperlb, lowerlt, lowerlb)

        );
    }

    private List<Double> extractContour(List<FirebaseVisionPoint> contours) {
        List<Double> output = new ArrayList<>();
        for (FirebaseVisionPoint p : contours) {
            output.add(p.getX().doubleValue());
            output.add(p.getY().doubleValue());
        }
        return output;
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

        for (int i = 0; i < arr.size(); i++) {
            sum += arr.get(i);
        }
        return sum / arr.size();
    }

    private Double varianceOf(List<Double> arr) {
        double x = 0.0;
        Double mean = averageOf(arr);

        for (int i = 0; i < arr.size(); i++) {
            x += Math.pow((arr.get(i) - mean), 2);
        }
        return x / arr.size();
    }

    private Double slopeOf(List<FirebaseVisionPoint> points, FacialFeature facialFeature) {

        int i1 = 0;
        int i2 = 0;

        if (facialFeature == FacialFeature.LEFT_EYE) {
            i2 = 8;
        } else if (facialFeature == FacialFeature.RIGHT_EYE) {
            i2 = 8;
        } else if (facialFeature == FacialFeature.LOWER_LIP_TOP) {
            i1 = 8;
        } else if (facialFeature == FacialFeature.LOWER_LIP_BOT) {
            i1 = 8;
        } else if (facialFeature == FacialFeature.UPPER_LIP_BOT) {
            i1 = 8;
        } else {
            i2 = 10;
        }

        FirebaseVisionPoint point1 = points.get(i1);
        FirebaseVisionPoint point2 = points.get(i2);

        return (double) Math.abs((point2.getY() - point1.getY()) / (point2.getX() - point1.getX()));
    }

    private <T> List<T> flattenList(List<List<T>> nested) {
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
