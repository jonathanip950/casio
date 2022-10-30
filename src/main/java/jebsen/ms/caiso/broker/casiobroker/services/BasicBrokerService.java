package jebsen.ms.caiso.broker.casiobroker.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jebsen.ms.caiso.broker.casiobroker.config.BasicBrokerConfig;
import jebsen.ms.caiso.broker.casiobroker.config.EmailConfig;
import jebsen.ms.caiso.broker.casiobroker.config.RetryJobConfig;
import jebsen.ms.caiso.broker.casiobroker.entites.RequestMethod;
import jebsen.ms.caiso.broker.casiobroker.entites.RequestRecord;
import jebsen.ms.caiso.broker.casiobroker.entites.RequestRecordRepository;
import jebsen.ms.email.models.SimpleEmail;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public abstract class BasicBrokerService {

    @Value("${spring.profiles.active}")
    protected String mode;
    @Autowired
    protected RequestRecordRepository requestRecordRepository;
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected ResponseEntity<String> responseEntity;
    protected RequestRecord requestRecord;
    protected RequestMethod requestMethod;
    @Autowired
    protected EmailService emailService;
    @Autowired
    protected EmailConfig emailConfig;

    public <T extends BasicBrokerConfig> ResponseEntity<String> handleRequest(JsonNode requestBodyJson, String method, T basicBrokerConfig) throws Exception {
        log.info("Start handling {} call, request: {}", basicBrokerConfig.getDestination(), requestBodyJson.toString());
        // basicBrokerConfig.getDestination() = axoms/oracle
        SimpleEmail simpleEmail = getEmailTemplate(basicBrokerConfig.getDestination());
        this.requestMethod = basicBrokerConfig.getMethods().stream()
                .filter(m -> method.equals(m.getName()))
                .findFirst()
                .orElse(null);

        if (this.requestMethod != null) {
            //For non_idempotent requests, check if request is successes before, if so return the successful response from Database.
            if (this.requestMethod.getRetryMode().equals(RetryJobConfig.NON_IDEMPOTENT)) {
                ResponseEntity<String> lastSuccessResponse = getLastSuccessResponse(this.requestMethod, requestBodyJson, basicBrokerConfig.getDestination());
                if (lastSuccessResponse != null) {
                    log.info("Last Success Response found, returning response for {} call, request: {} , response: {}", basicBrokerConfig.getDestination(), requestBodyJson.toString(), lastSuccessResponse.getBody());
                    return lastSuccessResponse;
                }
            }
        } else {
            log.warn(String.format("%s method not found in config: %s", basicBrokerConfig.getDestination().toString(), method));
        }

        //Store the request at db, if the request is duplicate, old one is replaced by the new one
        this.requestRecord = saveRequestRecord(requestMethod, requestBodyJson, basicBrokerConfig.getDestination());

        try {
            //call destination
            this.responseEntity = submitRequest(requestBodyJson);

            //Store the response and update status at db
            Boolean isSuccess = (requestMethod != null) ? checkResponseSuccess(responseEntity, requestMethod) : null;

            if (null != isSuccess && !isSuccess) {
                simpleEmail.pushContentRow("Response http code", this.responseEntity.getStatusCode().toString());
                simpleEmail.pushContentRow("Destination", basicBrokerConfig.getDestination());
                simpleEmail.pushContentRow("Request", requestBodyJson);
                simpleEmail.pushContentRow("Response body", this.responseEntity.getBody());
            }

            RequestRecord resultRequestRecord = updateRequestRecord(isSuccess, getResponseStr(responseEntity));
            log.info("Finish handling {} call, : {}", basicBrokerConfig.getDestination(), resultRequestRecord.toString());

        } catch (HttpStatusCodeException e) {
            log.error("Calling {} with error, status code :{}", basicBrokerConfig.getDestination(), e.getStatusCode(), e);
            simpleEmail.pushContentRow("Response Http code", e.getStatusCode().toString());
            simpleEmail.pushContentRow("Destination", basicBrokerConfig.getDestination());
            simpleEmail.pushContentRow("Request", requestBodyJson);
            simpleEmail.pushContentRow("Error Response", e.getResponseBodyAsString());
            this.responseEntity = ResponseEntity.status(e.getStatusCode()).header("Content-Type", "text/html; charset=utf-8").body(e.getResponseBodyAsString());
        }

        sendAlertEmailIfNeed(simpleEmail);
        return responseEntity;
    }

    protected ResponseEntity<String> getLastSuccessResponse(RequestMethod requestMethod, JsonNode requestBodyJson, RetryJobConfig.Destination destination) {
        if (requestMethod != null) {
            Pair<String, String> requestKeyValuePair = getRequestKeyValuePair(requestMethod, requestBodyJson);
            String key = requestKeyValuePair.getKey();
            String keyValue = requestKeyValuePair.getValue();
            return requestRecordRepository.findFirstByKeyAndKeyValueAndMethod(key, keyValue, requestMethod.getName()).map(rr -> {
                if (null != rr.getSuccess() && rr.getSuccess()) {
                    HttpHeaders responseHeaders = new HttpHeaders();
                    switch (destination.toString()) {
                        case RetryJobConfig.DESTINATION_AXOMS:
                            responseHeaders.set("Content-Type", "text/html; charset=utf-8");
                            responseHeaders.set("Company", "jcp");
                            responseHeaders.set("Cache-Control", "private");
                            responseHeaders.set("Partition", "initial");
                            return ResponseEntity.ok().headers(responseHeaders).body(rr.getResponse());
                        case RetryJobConfig.DESTINATION_ORACLE:
                            responseHeaders.set("Vary", "Accept-Encoding");
                            responseHeaders.set("Connection", "close");
                            responseHeaders.set("Content-Type", "application/json");
                            return ResponseEntity.ok().headers(responseHeaders).body(rr.getResponse());
                        default:
                            return ResponseEntity.ok().header("Content-Type", "application/json").body(rr.getResponse());
                    }
                }
                return null;
            }).orElse(null);
        } else {
            return null;
        }
    }

    protected Pair<String, String> getRequestKeyValuePair(RequestMethod requestMethod, JsonNode requestBodyJson) {
        String keys = requestMethod.getKey();
        List<String> keyArr = Arrays.asList(keys.split(","));
        String key = keyArr.stream()
                .filter((k) -> (requestBodyJson.findValue(k) != null))
                .findFirst()
                .orElseGet(null);
        String keyValue = requestBodyJson.findValue(key).textValue();
        return new MutablePair<>(key, keyValue);
    }

    protected RequestRecord saveRequestRecord(RequestMethod requestMethod, JsonNode requestBodyJson, RetryJobConfig.Destination destination) {
        if (requestMethod != null) {
            Pair<String, String> requestKeyValuePair = getRequestKeyValuePair(requestMethod, requestBodyJson);
            String key = requestKeyValuePair.getKey();
            String keyValue = requestKeyValuePair.getValue();

            Optional<RequestRecord> requestRecordOptional = requestRecordRepository.findFirstByKeyAndKeyValueAndMethod(key, keyValue, requestMethod.getName());
            return requestRecordOptional
                    .map(rr -> {
                        if (requestMethod.getRetryMode().equals(RetryJobConfig.IDEMPOTENT)) {
                            rr.setDestination(destination.toString());
                            rr.setRequest(requestBodyJson.toString());
                            rr.setSuccess(null);
                            rr.setResponse(null);
                            rr.setRetryMode(requestMethod.getRetryMode());
                            rr = requestRecordRepository.save(rr);
                        }
                        return rr;
                    })
                    .orElseGet(() -> {
                        RequestRecord rr = requestRecordRepository.save(RequestRecord.builder()
                                .destination(destination.toString())
                                .key(key)
                                .keyValue(keyValue)
                                .method(requestMethod.getName())
                                .request(requestBodyJson.toString())
                                .retryMode(requestMethod.getRetryMode())
                                .build());
                        return rr;
                    });
        } else {
            return requestRecordRepository.save(RequestRecord.builder()
                    .destination(destination.toString())
                    .key(null)
                    .keyValue(null)
                    .method(null)
                    .request(requestBodyJson.toString())
                    .retryMode(null)
                    .build());
        }
    }

    protected RequestRecord updateRequestRecord(Boolean isSuccess, String responseStr) {
        requestRecord.setSuccess(isSuccess);
        requestRecord.setResponse(responseStr);
        return requestRecordRepository.save(requestRecord);
    }

    protected abstract boolean checkResponseSuccess(ResponseEntity<String> responseEntity, RequestMethod requestMethod);

    protected abstract String getResponseStr(ResponseEntity<String> responseEntity);

    protected abstract ResponseEntity<String> submitRequest(JsonNode requestBody) throws HttpStatusCodeException;

    protected SimpleEmail getEmailTemplate(RetryJobConfig.Destination destination) {
        return SimpleEmail.builder()
                .title(String.format("<%s@%s> Calling %s with error response", "CasioBroker", mode.toUpperCase() ,destination.toString()))
                .sentFrom(String.format("no-reply@oms.%s", "casioBroker"))
                .recipients(emailConfig.getRecipients())
                .lineBreakSize(2)
                .build();
    }

    protected void sendAlertEmailIfNeed(SimpleEmail simpleEmail) {
        try {
            if (ObjectUtils.isNotEmpty(simpleEmail) && ObjectUtils.isNotEmpty(simpleEmail.getContentRows()) && ObjectUtils.isNotEmpty(simpleEmail.getRecipients())) {
                emailService.sendSimpleEmail(simpleEmail);
            }
        } catch (Exception e) {
            log.error("Error during sending email", e);
        }
    }

}
