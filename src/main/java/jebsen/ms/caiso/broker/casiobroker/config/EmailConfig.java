package jebsen.ms.caiso.broker.casiobroker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("email")
@Data
public class EmailConfig {
    List<String> recipients;
    String host;
}
