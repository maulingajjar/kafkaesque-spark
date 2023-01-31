/**
 * 
 */
package com.kaspar.model.spark;

import java.io.File;
import java.util.Arrays;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.factory.Nd4j;

/**
 *
 */
public class LocalSpark {

    private static final String[] CLASSES = new String[] { "Normal", "Cyclic", "Increasing Trend", "Decreasing Trend",
            "Upward Shift", "Downward Shift", };

    public static void main(String[] args) throws Exception {
        SparkSession spark = SparkSession.builder().master("local").appName("KasparStreamingLocal").getOrCreate();

        spark.sparkContext().setLogLevel("ERROR");

        SerializableModel model = new SerializableModel(
                MultiLayerNetwork.load(new File("src/main/resources/model/LSTMClassifier"), false));
        DataNormalization normalizer = NormalizerSerializer.getDefault()
                .restore(new File("src/main/resources/model/DataNormalizer"));

        spark.udf().register("kasparModel", new UDF1<String, String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String call(String message) throws Exception {
                double[] timeseries = Arrays.stream(message.split(",")).mapToDouble(Double::valueOf).toArray();
                INDArray input = Nd4j.create(timeseries).reshape(1, 1, timeseries.length);
                normalizer.transform(input);
                INDArray labelMask = Nd4j.zeros(1, 60).put(0, 59, 1);
                INDArray output = model.get().output(input, false, null, labelMask);
                output = output.tensorAlongDimension(59, 0, 1).reshape(6);
                return CLASSES[Nd4j.argMax(output, 0).toIntVector()[0]];
            }
        }, DataTypes.StringType);

        spark.udf().register("SplitModel", new UDF1<String, String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String call(String message) throws Exception {
                return message.split(",")[0];
            }
        }, DataTypes.StringType);

//        spark.readStream().format("kafka").option("kafka.bootstrap.servers", args[0]).option("subscribe", args[1])
//                .load().selectExpr("CAST(key AS STRING) AS key", "KasparModel(CAST(value AS STRING)) AS value")
//                .writeStream().format("console").outputMode(OutputMode.Append()).start().awaitTermination();

        spark.readStream().format("kafka").option("kafka.bootstrap.servers", args[0]).option("subscribe", args[1])
                .load().selectExpr("CAST(key AS STRING) AS key", "KasparModel(CAST(value AS STRING)) AS value")
                .writeStream().format("kafka").option("kafka.bootstrap.servers", args[0]).option("topic", args[2])
                .option("checkpointLocation", "/tmp/kafka_checkpoint").start().awaitTermination();

    }
}
