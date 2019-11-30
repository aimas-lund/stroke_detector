package com.passivestrokedetector;

import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;

import java.util.ArrayList;
import java.util.List;

public class FaceContours {

    private List<List<FirebaseVisionPoint>> facialFeatures = new ArrayList<>(4);

    FaceContours(FirebaseVisionFace face) {
        facialFeatures.add(face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints());
        facialFeatures.add(face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints());
        facialFeatures.add(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints());
        facialFeatures.add(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints());
    }

    public List<FirebaseVisionPoint> getLeftEyeContours() {
        return facialFeatures.get(0);
    }

    public List<FirebaseVisionPoint> getRightEyeContours() {
        return facialFeatures.get(1);
    }

    public List<FirebaseVisionPoint> getUpperLipContours() {
        return facialFeatures.get(2);
    }

    public List<FirebaseVisionPoint> getLowerLipContours() {
        return facialFeatures.get(3);
    }

    public List<List<FirebaseVisionPoint>> getAllContours() {
        return facialFeatures;
    }

}
