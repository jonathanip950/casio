package jebsen.ms.caiso.broker.casiobroker.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Component
@ConfigurationProperties("oracle")
@Data
public class OracleConfig extends BasicBrokerConfig {
    String username;
    String password;

    @Override
    public RetryJobConfig.Destination getDestination() {
        return RetryJobConfig.Destination.ORACLE;
    }


    public static boolean isOracleWorkingHours(LocalDateTime currentDateTime) {
        var isOfflineHours = (currentDateTime.getHour() >= 1 && currentDateTime.getHour() <= 4);
        return !isOfflineHours;
    }
}
