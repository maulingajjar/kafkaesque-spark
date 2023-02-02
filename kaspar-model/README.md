# kaspar-model
This module is a Maven/Java project which downloads the dataset from UCI corpus and trains an LSTM RNN classifer. This module also contains the code which converts the LSTM classifier into a Spark UDF that can be used within a Spark Structured Streaming application.

## Prerequisites
This module requires [Java 11](https://openjdk.org/projects/jdk/11/) and [Maven 3.5](https://maven.apache.org/docs/3.5.0/release-notes.html).

## Data Preparation
We will use the data from the UCI Synthetic Control Chart Time Series Data Set available at [UCI archive](https://archive.ics.uci.edu/ml/machine-learning-databases/synthetic_control-mld/synthetic_control.data). This dataset contains 600 examples of control charts generated synthetically with single line per chart spanning 60 columns. The 600 sequences are split into train set of size 450, validation set of size 90, and test set of size 60.

The data preparation uses the following format: one time series per file, and a separate file for the labels. For example, train/features/0.csv is the features using with the labels file train/labels/0.csv. Because the data is a univariate time series, we only have one column in the CSV files. Furthermore, because we have only one label for each time series, the labels CSV files contain only a single value.

Data download and preparation is handled by [DataPreparation.java](src/main/java/com/kaspar/model/DataPreparation.java). Run this program to download the data which will be placed under `src/main/resources/data` folder.

## Model Training
We train a sequence classifier using a LSTM Recurrent Neural Network to classify the univariate time series as belonging to one of six categories: `Normal, Cyclic, Increasing trend, Decreasing trend, Upward shift, Downward shift`.

The raw data contain values that are too large for effective training, and need to be normalized. Normalization is conducted using NormalizerStandardize, based on statistics (mean, st.dev) collected on the training data only. Note that both the training data and validation/test data are normalized in the same way.

The data set here is very small, so we can't afford to use a large network with many parameters. We are using one small LSTM layer and one RNN output layer.

Model training and saving is handled by [LSTMClassifier.java](src/main/java/com/kaspar/model/LSTMClassifier.java). Run this program to train the model which will be saved under `src/main/resources/model` folder along with the data normalizer.

## Create Uber JAR
To run the Spark application we need to create an Uber JAR which contains all the required dependencies. Run the following command to create the application JAR which will be located under `target` folder of this module:
```
mvn clean install
```