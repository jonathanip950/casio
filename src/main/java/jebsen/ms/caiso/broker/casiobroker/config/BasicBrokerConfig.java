package jebsen.ms.caiso.broker.casiobroker.config;

import jebsen.ms.caiso.broker.casiobroker.entites.RequestMethod;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
public abstract class BasicBrokerConfig {

    List<RequestMethod> methods;
    String urlPrefix;

    public abstract RetryJobConfig.Destination getDestination();
}
