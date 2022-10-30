package jebsen.ms.caiso.broker.casiobroker.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class RetryJobConfig {
    public static final String NON_IDEMPOTENT = "non_idempotent";
    public static final String IDEMPOTENT = "idempotent";
    public static final String DESTINATION_AXOMS = "axoms";
    public static final String DESTINATION_ORACLE = "oracle";

    public enum BatchRetryMode {
        ALL("all"),
        IDEMPOTENT(RetryJobConfig.IDEMPOTENT),
        NON_IDEMPOTENT(RetryJobConfig.NON_IDEMPOTENT);

        private final String value;

        BatchRetryMode(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum Destination {
        AXOMS(DESTINATION_AXOMS),
        ORACLE(DESTINATION_ORACLE);
        private final String value;

        Destination(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
