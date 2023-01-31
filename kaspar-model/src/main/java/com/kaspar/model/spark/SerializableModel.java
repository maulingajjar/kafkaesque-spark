/**
 * 
 */
package com.kaspar.model.spark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

/**
 *
 */
public class SerializableModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private transient MultiLayerNetwork model;

    private byte[] serializedModel;

    public SerializableModel(MultiLayerNetwork model) {
        this.model = model;
    }

    /**
     * @param outputStream object output stream
     * @throws IOException IO Exception
     */
    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        serialize();
        outputStream.defaultWriteObject();
    }

    /**
     * @param inputStream object input stream
     * @throws IOException            IO Exception
     * @throws ClassNotFoundException class not found exception
     */
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        deserialize();
    }

    /**
     * Serialize.
     */
    private void serialize() throws IOException {
        // Model instances are immutable. Serialization should only happen once.
        if (serializedModel == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            ModelSerializer.writeModel(model, baos, false);
            serializedModel = baos.toByteArray();
        }
    }

    /**
     * De-serialize.
     */
    private void deserialize() throws IOException {
        model = ModelSerializer.restoreMultiLayerNetwork(new ByteArrayInputStream(serializedModel));
    }

    /**
     * @return the model
     * @throws IOException
     */
    public MultiLayerNetwork get() throws IOException {
        if (model == null) {
            deserialize();
        }
        return model;
    }
}
