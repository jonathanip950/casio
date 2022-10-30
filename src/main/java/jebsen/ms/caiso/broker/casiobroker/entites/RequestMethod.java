package jebsen.ms.caiso.broker.casiobroker.entites;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@Component
@AllArgsConstructor
@NoArgsConstructor
public class RequestMethod {
    private String name;
    private String key;
    private String retryMode;
}
