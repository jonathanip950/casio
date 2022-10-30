package jebsen.ms.caiso.broker.casiobroker.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties("axoms")
@Data
public class AxMethodConfig extends BasicBrokerConfig {

    @Override
    public RetryJobConfig.Destination getDestination() {
        return RetryJobConfig.Destination.AXOMS;
    }
}
