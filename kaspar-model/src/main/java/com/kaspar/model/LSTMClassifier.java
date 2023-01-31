/* *****************************************************************************
 *
 *
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package com.kaspar.model;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.InvocationType;
import org.deeplearning4j.optimize.listeners.EvaluativeListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.learning.config.Nadam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import com.kaspar.model.DataPreparation.Type;

/**
 * Train a sequence classifier using a LSTM Recurrent Neural Network to classify
 * univariate time series as belonging to one of six categories: Normal, Cyclic,
 * Increasing trend, Decreasing trend, Upward shift, Downward shift.
 * <p>
 * 
 * {@link DataPreparation} downloads and prepares the data.
 * <p>
 * 
 * The raw data contain values that are too large for effective training, and
 * need to be normalized. Normalization is conducted using
 * NormalizerStandardize, based on statistics (mean, st.dev) collected on the
 * training data only. Note that both the training data and validation/test data
 * are normalized in the same way.
 * <p>
 * 
 * The data set here is very small, so we can't afford to use a large network
 * with many parameters. We are using one small LSTM layer and one RNN output
 * layer.
 */
public class LSTMClassifier {

    private static final Logger LOGGER = Logger.getLogger(LSTMClassifier.class.getName());

    private static final int NUM_LABELS = 6;

    private static final int NUM_EPOCHS = 30;

    public static void main(String[] args) throws Exception {

        // ----- Load the training data -----
        // Note that we have 450 training files for features: train/features/0.csv
        // through train/features/449.csv
        DataSetIterator trainData = getDataSetIterator(Type.train, 0, 449);

        // Normalize the data
        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(trainData);
        // Collect data statistics
        trainData.reset();

        // Use previously collected statistics to normalize on-the-fly. Each DataSet
        // returned by iterator will be normalized
        trainData.setPreProcessor(normalizer);

        // ----- Load the validation data -----
        // Same process as for the training data. Note that we have 90 validation files
        // for features: validate/features/0.csv through validate/features/89.csv
        DataSetIterator validateData = getDataSetIterator(Type.validate, 0, 89);
        validateData.setPreProcessor(normalizer);

        // ----- Configure the network -----
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                // Random number generator seed for improved repeatability.
                .seed(123).weightInit(WeightInit.XAVIER).updater(new Nadam())
                // Not always required, but helps with this data set
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                .gradientNormalizationThreshold(0.5).list()
                .layer(new LSTM.Builder().activation(Activation.TANH).nIn(1).nOut(10).build())
                .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT).activation(Activation.SOFTMAX)
                        .nIn(10).nOut(NUM_LABELS).build())
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();

        LOGGER.info("Starting training...");
        // Print the score (loss function value) every 20 iterations
        net.setListeners(new ScoreIterationListener(20),
                new EvaluativeListener(validateData, 1, InvocationType.EPOCH_END));
        net.fit(trainData, NUM_EPOCHS);

        LOGGER.info("Evaluating...");
        // ----- Load the test data -----
        // Same process as for the training and validation data. Note that we have 60
        // validation files for features: test/features/0.csv through
        // test/features/59.csv
        DataSetIterator testData = getDataSetIterator(Type.test, 0, 59);
        testData.setPreProcessor(normalizer);
        Evaluation eval = net.evaluate(testData);
        LOGGER.info(eval.stats());

        LOGGER.info("Saving the model...");
        net.save(new File("src/main/resources/model/LSTMClassifier"));
        LOGGER.info("Saving the normalizer...");
        NormalizerSerializer.getDefault().write(normalizer, new File("src/main/resources/model/DataNormalizer"));

        LOGGER.info("----- Example Complete -----");
    }

    private static DataSetIterator getDataSetIterator(DataPreparation.Type type, int minIndex, int maxIndex)
            throws Exception {
        File dataDir = new File("src/main/resources/data/");
        SequenceRecordReader features = new CSVSequenceRecordReader();
        features.initialize(new NumberedFileInputSplit(
                Paths.get(dataDir.getPath(), type.name(), "features") + "/%d.csv", minIndex, maxIndex));
        SequenceRecordReader labels = new CSVSequenceRecordReader();
        labels.initialize(new NumberedFileInputSplit(Paths.get(dataDir.getPath(), type.name(), "labels") + "/%d.csv",
                minIndex, maxIndex));

        DataSetIterator data = new SequenceRecordReaderDataSetIterator(features, labels, 10, NUM_LABELS, false,
                SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

        return data;
    }
}
