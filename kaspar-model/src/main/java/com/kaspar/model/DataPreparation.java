/**
 * 
 */
package com.kaspar.model;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.nd4j.common.primitives.Pair;

/**
 * Data is the UCI Synthetic Control Chart Time Series Data Set available
 * <a href=
 * "https://archive.ics.uci.edu/ml/machine-learning-databases/synthetic_control-mld/synthetic_control.data">here</a>.
 * <p>
 * 
 * This dataset contains 600 examples of control charts generated synthetically
 * with single line per chart spanning 60 columns. The 600 sequences are split
 * into train set of size 450, validation set of size 90, and test set of size
 * 60.
 * <p>
 * 
 * The data preparation uses the following format: one time series per file, and
 * a separate file for the labels. For example, train/features/0.csv is the
 * features using with the labels file train/labels/0.csv. Because the data is a
 * univariate time series, we only have one column in the CSV files.
 * Furthermore, because we have only one label for each time series, the labels
 * CSV files contain only a single value.
 */
public class DataPreparation {

    public enum Type {
        train, validate, test;
    }

    private static final Logger LOGGER = Logger.getLogger(DataPreparation.class.getName());

    private static final String DATA_URL = "https://archive.ics.uci.edu/ml/machine-learning-databases/synthetic_control-mld/synthetic_control.data";

    private static File dataDir = new File("src/main/resources/data/");

    // Paths for train, validate and test features, labels
    private static Path[][] paths = new Path[3][2];

    public static void main(String[] args) throws Exception {
        createDataDirs(Type.train.name(), Type.validate.name(), Type.test.name());
        downloadUCIData();
    }

    private static void createDataDirs(String... dirs) throws Exception {
        if (dataDir.exists()) {
            dataDir.delete();
        }
        int count = 0;
        for (String dir : dirs) {
            Path path = Files.createDirectories(Paths.get(dataDir.toPath().toString(), dir));
            paths[count][0] = Files.createDirectories(Paths.get(path.toString(), "features"));
            paths[count][1] = Files.createDirectories(Paths.get(path.toString(), "labels"));
            count++;
        }
    }

    // Download the data and convert the "one time series per line"
    // format into a suitable CSV sequence format that DataVec
    // (CsvSequenceRecordReader) and DL4J can read.
    private static void downloadUCIData() throws Exception {

        String data = IOUtils.toString(new URL(DATA_URL), (Charset) null);
        String[] lines = data.split("\n");

        int lineCount = 0;
        List<Pair<String, Integer>> contentAndLabels = new ArrayList<>();
        for (String line : lines) {
            String transposed = line.replaceAll(" +", "\n");

            // Labels: first 100 (lines) are label 0, second 100 are label 1, and so on
            contentAndLabels.add(new Pair<>(transposed, lineCount++ / 100));
        }

        // Randomize and do a train/test split:
        Collections.shuffle(contentAndLabels, new Random(12345));

        // 75% train, 15% validate, 10% test
        int nTrain = 450;
        int nValidate = 90;
        int trainCount = 0;
        int validateCount = 0;
        int testCount = 0;

        // Files containing features in original format
        File originalTrain = new File(paths[0][0].getParent().toString(), "kaspar.csv");
        File originalValidate = new File(paths[1][0].getParent().toString(), "kaspar.csv");
        File originalTest = new File(paths[2][0].getParent().toString(), "kaspar.csv");

        for (Pair<String, Integer> p : contentAndLabels) {
            // Write output in a format we can read, in the appropriate locations
            File outPathFeatures;
            File outPathLabels;
            if (trainCount < nTrain) {
                FileUtils.writeStringToFile(originalTrain, p.getFirst().replaceAll("\n", ",") + "\n", (Charset) null,
                        true);
                outPathFeatures = new File(paths[0][0].toString(), trainCount + ".csv");
                outPathLabels = new File(paths[0][1].toString(), trainCount + ".csv");
                trainCount++;
            } else if (validateCount < nValidate) {
                FileUtils.writeStringToFile(originalValidate, p.getFirst().replaceAll("\n", ",") + "\n", (Charset) null,
                        true);
                outPathFeatures = new File(paths[1][0].toString(), validateCount + ".csv");
                outPathLabels = new File(paths[1][1].toString(), validateCount + ".csv");
                validateCount++;
            } else {
                FileUtils.writeStringToFile(originalTest, p.getFirst().replaceAll("\n", ",") + "\n", (Charset) null,
                        true);
                outPathFeatures = new File(paths[2][0].toString(), testCount + ".csv");
                outPathLabels = new File(paths[2][1].toString(), testCount + ".csv");
                testCount++;
            }

            FileUtils.writeStringToFile(outPathFeatures, p.getFirst(), (Charset) null);
            FileUtils.writeStringToFile(outPathLabels, p.getSecond().toString(), (Charset) null);
        }
        LOGGER.info("Downloaded " + trainCount + " samples for training under " + paths[0][0].getParent().toString());
        LOGGER.info(
                "Downloaded " + validateCount + " samples for validation under " + paths[1][0].getParent().toString());
        LOGGER.info("Downloaded " + testCount + " samples for testing under " + paths[2][0].getParent().toString());
    }

}
