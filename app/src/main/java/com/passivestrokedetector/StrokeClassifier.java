package com.passivestrokedetector;

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

        return new Instances("modelInstances", attrs, 10000);
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
        Instance instance = new Instance((numFeatures + 1)*allFeatures.size());

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
    public void addToInstances(Instance instance) {

        Attribute attrClass = instances.attribute("label");
        instances.setClass(attrClass);
        instances.add(instance);
    }

    public void train() throws Exception {
        classifier.buildClassifier(instances);
        Log.d(TAG, "Model trained");
    }

    public String predict(Instance instance) throws Exception {
        double result = classifier.classifyInstance(instance);
        String output = instances.classAttribute().value((int) result);

        Log.d(TAG, "Prediction made: " + output);
        return output;
    }

    /*
    Fetches classifier model
     */
    public void save(String fileName) throws IOException {
        ArffSaver saver = new ArffSaver();

        saver.setInstances(instances);

        String dirPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = dirPath + fileName;

        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        saver.setFile(new File(filePath));
        saver.writeBatch();
    }

    public void load(String fileName) throws Exception {
        //String dirPath = "/sdcard/classifierModel";
        String dirPath = Environment.getExternalStorageDirectory().getPath();
        String filePath = dirPath + fileName;

        if (!new File(filePath).exists()) {
            throw new FileNotFoundException(fileName + " does not exist");
        }

        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        Instances data = new Instances(reader);

        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);
        train();
    }

    public void delete(String fileName) {

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

    public Boolean checkModelAvailable(String fileName) {
        String dirPath = Environment.getExternalStorageDirectory().getPath();
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

    public List<String> getAllFeaturesFlattened() {
        return flattenList(allFeatures);
    }

    public String getTAG() {
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
            default:        return "";
        }
    }

}
