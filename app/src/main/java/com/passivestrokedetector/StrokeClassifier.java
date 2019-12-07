package com.passivestrokedetector;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.classifiers.bayes.NaiveBayes;

public class StrokeClassifier {

    private static String TAG = "Stroke Classifier";

    private int numFeatures = 9;
    private int numClasses = 2;
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
    public Instance createInstance(List<String> attrs, List<Double> values, String className) {
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

        Log.d(TAG, "Prediction made: '$output'");
        return output;
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

}
