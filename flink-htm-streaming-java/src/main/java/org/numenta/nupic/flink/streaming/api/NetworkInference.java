package org.numenta.nupic.flink.streaming.api;

import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import org.numenta.nupic.algorithms.Classification;
import org.numenta.nupic.flink.serialization.KryoSerializer;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.Network;

import java.util.HashMap;
import java.util.Map;

/**
 * Container for output from a {@link Network}. Represents the
 * result accumulated by the computation of a sequence of algorithms
 * contained in the network.
 */
public class NetworkInference {

    @FieldSerializer.Bind(DefaultSerializers.DoubleSerializer.class)
    private final double anomalyScore;

    @MapSerializer.BindMap(
            keySerializer = DefaultSerializers.StringSerializer.class, keyClass = String.class,
            valueSerializer = KryoSerializer.class, valueClass = Classification.class)
    private final Map<String, Classification<Object>> classifications;

    public NetworkInference(double anomalyScore, Map<String, Classification<Object>> classifications) {
        this.anomalyScore = anomalyScore;
        this.classifications = classifications;
    }

    public double getAnomalyScore() {
        return this.anomalyScore;
    }

    /**
     * Returns the most recent {@link Classification}
     *
     * @param fieldName the field for which to get the classification.
     * @return the classification result.
     */
    public Classification<Object> getClassification(String fieldName) {
        if (classifications == null) throw new IllegalStateException("no classification results are available");
        return classifications.get(fieldName);
    }

    public static NetworkInference fromInference(Inference i) {
        Map<String, Classification<Object>> classifications = new HashMap<>();
        for (String field : i.getClassifiers().keys()) {
            classifications.put(field, i.getClassification(field));
        }
        return new NetworkInference(i.getAnomalyScore(), classifications);
    }
}