package com.passivestrokedetector;

import java.util.Arrays;
import java.util.List;

import weka.core.FastVector;
import weka.core.Instances;

public class StrokeClassifier {

    private int numFeatures = 4;
    private int numClasses = 2;
    private List<Float> listFeature;
    private List<Float> listClass;

    private Instances instances = createEmptyInstances();

    private Instances createEmptyInstances() {
        /* Create an empty list of instances */
        FastVector attrs = new FastVector();

        for (f in listFeature) {
            attrs.addElement(Attribute(f))
        }

        val classes = FastVector()
        for (c in listClass) {
            classes.addElement(c)
        }
        attrs.addElement(Attribute("label", classes))

        return Instances("myInstances", attrs, 10000)
    }




}
