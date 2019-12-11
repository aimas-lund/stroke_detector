package com.passivestrokedetector;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Environment;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.converters.ArffSaver;

public class StrokeClassifier {

    private static String TAG = "Stroke Classifier";

    private int numFeatures = 9;
    private List<String> listFeatureLeftEye = initList("le");
    private List<String> listFeatureRightEye = initList("re");
    private List<String> listFeatureLowerLip = initList("ll");
    private List<String> listFeatureUpperLip = initList("ul");
    private List<List<String>> allFeatures =
            Arrays.asList(
            listFeatureLeftEye,
            listFeatureRightEye,
            listFeatureLowerLip,
            listFeatureUpperLip);
    private List<String> listClass = Arrays.asList("Normal", "Drooping");
    private Instances instances = createEmptyInstances();

    // For training
    private NaiveBayes classifier = new NaiveBayes();

    // Functions
    private Instances createEmptyInstances() {
        /* Create an empty list of instances */
        FastVector attrs = new FastVector();

        for (List<String> list : allFeatures) {
            for (String f : list) {
                attrs.addElement(new Attribute(f));
            }
        }

        FastVector classes = new FastVector();

        for (String c : listClass) {
            classes.addElement(c);
        }
        attrs.addElement(new Attribute("label", classes));

        Instances instances = new Instances("modelInstances", attrs, 50);
        Attribute attr = instances.attribute("label");
        instances.setClass(attr);

        return instances;
    }

    /*
    Be sure that the lists contains values from all the facial features in the following order:
    left eye, right eye, lower lip, upper lip
     */
    Instance createInstance(List<String> attrs,
                            List<Double> values,
                            StateOfFace faceState) {

        String className = className(faceState);

        Attribute attrClass = instances.attribute("label");
        Instance instance = new Instance(attrs.size() + 1);

        for (int j = 0; j < attrs.size(); j++) {
            Attribute attrInstance = instances.attribute(attrs.get(j));
            instance.setValue(attrInstance, values.get(j));
        }
        instance.setValue(attrClass, className);

        return instance;
    }

    /*
    Takes whatever instance created and adds it in Instances class
    */
    void addToInstances(Instance instance) {
        Attribute classAttr = instances.attribute("label");
        instances.setClass(classAttr);
        instances.add(instance);
    }

    void train() throws Exception {
        classifier.buildClassifier(instances);
        Log.d(TAG, "Model trained");
    }

    String predict(Instance instance) throws Exception {
        instance.setDataset(instances);
        double result = classifier.classifyInstance(instance);
        String output = instances.classAttribute().value((int) result);

        Log.d(TAG, "Prediction made: " + output);
        return output;
    }

    /*
    Fetches classifier model
     */
    void save(String fileName) throws IOException {
        ArffSaver saver = new ArffSaver();

        saver.setInstances(instances);

        @SuppressLint("SdCardPath")
        String dirPath = "/sdcard/weka/";
        String filePath = dirPath + fileName;

        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        saver.setFile(new File(filePath));
        saver.writeBatch();
    }

    void load(String fileName) throws Exception {
        @SuppressLint("SdCardPath")
        String dirPath = "/sdcard/weka/";
        String filePath = dirPath + fileName;

        if (!new File(filePath).exists()) {
            throw new FileNotFoundException(fileName + " does not exist");
        }

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        Instances data = new Instances(reader);

        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }
        train();
    }

    void delete(String fileName) {

        if (checkModelAvailable(fileName)) {
            String dirPath = Environment.getExternalStorageDirectory().getPath();
            String filePath = dirPath + fileName;

            File file = new File(filePath);
            file.delete();
            Log.d(TAG, "Model successfully deleted");
        } else {
            Log.d(TAG, "No model with that name available");
        }
    }

    Boolean checkModelAvailable(String fileName) {
        @SuppressLint("SdCardPath")
        String dirPath = "/sdcard/weka/";
        String filePath = dirPath + fileName;

        return new File(filePath).exists();
    }

    private <T> List<T> flattenList(List<List<T>> nested) {
        List<T> output = new ArrayList<>();
        nested.forEach(output::addAll);

        return output;
    }

    public List<List<String>> getAllFeatures() {
        return allFeatures;
    }

    List<String> getAllFeaturesFlattened() {
        return flattenList(allFeatures);
    }

    String getTAG() {
        return TAG;
    }

    /*
    =====================================
    AUXILIARY FUNCTIONS
    =====================================
     */

    private List<String> initList(String featureName) {

        List<String> list = new ArrayList<>();

        for (int i = 0; i < numFeatures; i++) {
            list.add(featureName + i);
        }

        return list;
    }

    private String className(StateOfFace className) {

        switch (className) {
            case NORMAL:    return listClass.get(0);
            case DROOPING:  return listClass.get(1);
            default:        throw new IllegalStateException();
        }
    }

    public Instances getInstances() {
        return instances;
    }
}
