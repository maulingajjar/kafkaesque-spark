/**
 * 
 */
package com.kaspar.model.spark;

import java.util.Arrays;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.DataTypes;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.factory.Nd4j;

/**
 *
 */
public class KasparStreaming {

    private static final String[] CLASSES = new String[] { "Normal", "Cyclic", "Increasing Trend", "Decreasing Trend",
            "Upward Shift", "Downward Shift", };

    public static void main(String[] args) throws Exception {
        SparkSession spark = SparkSession.builder().appName("KasparStreaming").getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        // Even though MultiLayerNetwork is serializable, we use a custom Serializable
        // wrapper to avoid serialization issues in UDF closure.
        SerializableModel model = new SerializableModel(ModelSerializer
                .restoreMultiLayerNetwork(KasparStreaming.class.getResourceAsStream("/model/LSTMClassifier"), false));
        DataNormalization normalizer = NormalizerSerializer.getDefault()
                .restore(KasparStreaming.class.getResourceAsStream("/model/DataNormalizer"));

        spark.udf().register("KasparModel", new UDF1<String, String>() {

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

        spark.readStream().format("kafka").option("kafka.bootstrap.servers", args[0]).option("subscribe", args[1])
                .load().selectExpr("CAST(key AS STRING) AS key", "KasparModel(CAST(value AS STRING)) AS value")
                .writeStream().format("kafka").option("kafka.bootstrap.servers", args[0]).option("topic", args[2])
                // In case of a failure or intentional shutdown, recover the previous progress
                // and state of a previous query to continue where it left off using
                // checkpointing
                .option("checkpointLocation", "/tmp/kafka_checkpoint").start().awaitTermination();
    }
}
