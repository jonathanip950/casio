package jebsen.ms.caiso.broker.casiobroker.services;

import jebsen.ms.caiso.broker.casiobroker.config.EmailConfig;
import jebsen.ms.email.models.SimpleEmail;
import jebsen.ms.email.sdk.EmailSdk;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class EmailService {

    @Getter
    private jebsen.ms.email.sdk.EmailSdk emailSdk;

    @Autowired
    private EmailConfig emailConfig;

    @PostConstruct
    private void postInit() {
        emailSdk = EmailSdk.builder().domain(emailConfig.getHost()).build();
    }

    public void sendSimpleEmail(SimpleEmail simpleEmail) {
        var result = emailSdk.simpleEmail(simpleEmail).getBody();
        if (ObjectUtils.isNotEmpty(result)) {
            log.info("Sent e-mail. Success: {} ; Msg: {}", result.getSuccess(), result.getMsg());
        }
    }
}
