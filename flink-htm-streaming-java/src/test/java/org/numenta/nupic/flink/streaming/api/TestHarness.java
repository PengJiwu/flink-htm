package org.numenta.nupic.flink.streaming.api;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.*;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.util.MersenneTwister;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

/**
 * HTM models for test purposes.
 */
public class TestHarness {

    public static class DayDemoRecord {
        public int dayOfWeek;

        public DayDemoRecord() {}
        public DayDemoRecord(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    }

    public static class DayDemoRecordSourceFunction extends TestSourceFunction<DayDemoRecord> {

        private volatile int dayOfWeek = 0;

        public DayDemoRecordSourceFunction(int numCheckpoints, boolean failAfterCheckpoint) {
            super(numCheckpoints, failAfterCheckpoint);
        }

        @Override
        protected Supplier<DayDemoRecord> generate() {
            return new Supplier<DayDemoRecord>() {
                @Override
                public DayDemoRecord get() {
                    return new DayDemoRecord(dayOfWeek++ % 7);
                }
            };
        }

        @Override
        public Long snapshotState(long checkpointId, long checkpointTimestamp) throws Exception {
            super.snapshotState(checkpointId, checkpointTimestamp);
            return Long.valueOf(dayOfWeek);
        }

        @Override
        public void restoreState(Long state) throws Exception {
            super.restoreState(state);
            dayOfWeek = state.intValue();
        }
    }

    public static class DayDemoNetworkFactory implements NetworkFactory<TestHarness.DayDemoRecord> {
        @Override
        public Network createNetwork(Object key) {
            Parameters p = getParameters();
            p = p.union(getEncoderParams());
            p.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 30 });
            p.set(Parameters.KEY.SYN_PERM_INACTIVE_DEC, 0.1);
            p.set(Parameters.KEY.SYN_PERM_ACTIVE_INC, 0.1);
            p.set(Parameters.KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
            p.set(Parameters.KEY.SYN_PERM_CONNECTED, 0.4);
            p.set(Parameters.KEY.MAX_BOOST, 10.0);
            p.set(Parameters.KEY.DUTY_CYCLE_PERIOD, 7);
            p.set(Parameters.KEY.RANDOM, new MersenneTwister(42));

            Map<String, Object> params = new HashMap<>();
            params.put(KEY_MODE, Anomaly.Mode.PURE);

            Network n = Network.create("DayDemoNetwork", p)
                    .add(Network.createRegion("r1")
                            .add(Network.createLayer("1", p)
                                    .alterParameter(Parameters.KEY.AUTO_CLASSIFY, Boolean.TRUE)
                                    .alterParameter(Parameters.KEY.INFERRED_FIELDS, getInferredFieldsMaps())
                                    .add(new TemporalMemory())
                                    .add(new SpatialPooler())
                                    .add(MultiEncoder.builder().name("").build())));
            return n;
        }

        /**
         * Parameters and meta information for the "dayOfWeek" encoder
         * @return
         */
        public static Map<String, Map<String, Object>> getFieldEncodingMap() {
            Map<String, Map<String, Object>> fieldEncodings = setupMap(
                    null,
                    8, // n
                    3, // w
                    0.0, 8.0, 0, 1, Boolean.TRUE, null, Boolean.TRUE,
                    "dayOfWeek", "number", "ScalarEncoder");
            return fieldEncodings;
        }

        public static Map<String, Class<? extends Classifier>> getInferredFieldsMaps() {
            Map<String, Class<? extends Classifier>> map = new LinkedHashMap<>();
            map.put("dayOfWeek", CLAClassifier.class);
            return map;
        }

        /**
         * Returns Encoder parameters for the "dayOfWeek" test encoder.
         * @return
         */
        public static Parameters getEncoderParams() {
            Map<String, Map<String, Object>> fieldEncodings = getFieldEncodingMap();

            Parameters p = Parameters.getEncoderDefaultParameters();
            p.set(Parameters.KEY.FIELD_ENCODING_MAP, fieldEncodings);

            return p;
        }

        /**
         * Returns the default parameters used for the "dayOfWeek" encoder and algorithms.
         * @return
         */
        public static Parameters getParameters() {
            Parameters parameters = Parameters.getAllDefaultParameters();
            parameters.set(Parameters.KEY.INPUT_DIMENSIONS, new int[] { 8 });
            parameters.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 20 });
            parameters.set(Parameters.KEY.CELLS_PER_COLUMN, 6);

            //SpatialPooler specific
            parameters.set(Parameters.KEY.POTENTIAL_RADIUS, 12);//3
            parameters.set(Parameters.KEY.POTENTIAL_PCT, 0.5);//0.5
            parameters.set(Parameters.KEY.GLOBAL_INHIBITION, false);
            parameters.set(Parameters.KEY.LOCAL_AREA_DENSITY, -1.0);
            parameters.set(Parameters.KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
            parameters.set(Parameters.KEY.STIMULUS_THRESHOLD, 1.0);
            parameters.set(Parameters.KEY.SYN_PERM_INACTIVE_DEC, 0.01);
            parameters.set(Parameters.KEY.SYN_PERM_ACTIVE_INC, 0.1);
            parameters.set(Parameters.KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
            parameters.set(Parameters.KEY.SYN_PERM_CONNECTED, 0.1);
            parameters.set(Parameters.KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
            parameters.set(Parameters.KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
            parameters.set(Parameters.KEY.DUTY_CYCLE_PERIOD, 10);
            parameters.set(Parameters.KEY.MAX_BOOST, 10.0);
            parameters.set(Parameters.KEY.SEED, 42);

            //Temporal Memory specific
            parameters.set(Parameters.KEY.INITIAL_PERMANENCE, 0.2);
            parameters.set(Parameters.KEY.CONNECTED_PERMANENCE, 0.8);
            parameters.set(Parameters.KEY.MIN_THRESHOLD, 5);
            parameters.set(Parameters.KEY.MAX_NEW_SYNAPSE_COUNT, 6);
            parameters.set(Parameters.KEY.PERMANENCE_INCREMENT, 0.05);
            parameters.set(Parameters.KEY.PERMANENCE_DECREMENT, 0.05);
            parameters.set(Parameters.KEY.ACTIVATION_THRESHOLD, 4);

            return parameters;
        }
    }

    /**
     * Sets up an Encoder Mapping of configurable values.
     *
     * @param map               if called more than once to set up encoders for more
     *                          than one field, this should be the map itself returned
     *                          from the first call to {@code #setupMap(Map, int, int, double,
     *                          double, double, double, Boolean, Boolean, Boolean, String, String, String)}
     * @param n                 the total number of bits in the output encoding
     * @param w                 the number of bits to use in the representation
     * @param min               the minimum value (if known i.e. for the ScalarEncoder)
     * @param max               the maximum value (if known i.e. for the ScalarEncoder)
     * @param radius            see {@link Encoder}
     * @param resolution        see {@link Encoder}
     * @param periodic          such as hours of the day or days of the week, which repeat in cycles
     * @param clip              whether the outliers should be clipped to the min and max values
     * @param forced            use the implied or explicitly stated ratio of w to n bits rather than the "suggested" number
     * @param fieldName         the name of the encoded field
     * @param fieldType         the data type of the field
     * @param encoderType       the Camel case class name minus the .class suffix
     * @return
     */
    public static Map<String, Map<String, Object>> setupMap(
            Map<String, Map<String, Object>> map,
            int n, int w, double min, double max, double radius, double resolution, Boolean periodic,
            Boolean clip, Boolean forced, String fieldName, String fieldType, String encoderType) {

        if(map == null) {
            map = new HashMap<String, Map<String, Object>>();
        }
        Map<String, Object> inner = null;
        if((inner = map.get(fieldName)) == null) {
            map.put(fieldName, inner = new HashMap<String, Object>());
        }

        inner.put("n", n);
        inner.put("w", w);
        inner.put("minVal", min);
        inner.put("maxVal", max);
        inner.put("radius", radius);
        inner.put("resolution", resolution);

        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clipInput", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);

        return map;
    }
}
