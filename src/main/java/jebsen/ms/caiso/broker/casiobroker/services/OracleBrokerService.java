package jebsen.ms.caiso.broker.casiobroker.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import jebsen.ms.caiso.broker.casiobroker.config.OracleConfig;
import jebsen.ms.caiso.broker.casiobroker.entites.RequestMethod;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j

public class OracleBrokerService extends BasicBrokerService {

    private final OracleConfig oracleConfig;

    public ResponseEntity<String> handleRequest(JsonNode requestBodyJson, String method) throws Exception {
        return super.handleRequest(requestBodyJson, method, oracleConfig);
    }

    @Override
    protected boolean checkResponseSuccess(ResponseEntity<String> responseEntity, RequestMethod requestMethod) {
        try {
            JsonNode requestBodyJson = objectMapper.readTree(getResponseStr(responseEntity));
            switch (requestMethod.getName()) {
                case "create_online_order":
                    return (requestBodyJson.findValue("ORA_ORDER_NUMBER") != null) && !requestBodyJson.findValue("ORA_ORDER_NUMBER").asText().equals("-1");
                case "update_delivery_status":
                case "update_delivery_info":
                    return (requestBodyJson.findValue("MAG_ORDER_ID") != null) && !Strings.isNullOrEmpty(requestBodyJson.findValue("MAG_ORDER_ID").asText());
                default:
                    return false;
            }
        } catch (JsonProcessingException e) {
            log.error("checkResponseSuccess: Fail to parse Oracle response body", e);
            return false;
        }
    }

    @Override
    protected String getResponseStr(ResponseEntity<String> responseEntity) {
        String result;
        try {
            result = objectMapper.readTree(Objects.requireNonNull(responseEntity.getBody(), "Response body is null")).toString();
        } catch (JsonProcessingException e) {
            log.error("getResponseStr: Fail to parse Oracle response body when", e);
            result = "Fail to parse Oracle response body";
        }
        return result;
    }

    @Override
    protected ResponseEntity<String> submitRequest(JsonNode requestBodyJson) throws HttpStatusCodeException {
        HttpHeaders headers = new HttpHeaders();
        String auth = oracleConfig.getUsername() + ":" + oracleConfig.getPassword();
        byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.US_ASCII));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Content-Type", "application/json");

        String jsonString = requestBodyJson.toString();

        HttpEntity httpEntity = new HttpEntity<>(jsonString, headers);

        String finalUrl = String.format("%s%s/", oracleConfig.getUrlPrefix(), this.requestMethod.getName());

        log.info("Calling Oracle: {} , Request body: {}", finalUrl, jsonString);

//        try {
            return new RestTemplate().exchange(URI.create(finalUrl), HttpMethod.POST, httpEntity, String.class);
//        } catch (HttpStatusCodeException e) {
//            log.error("Calling Oracle with error, status code :{}", e.getStatusCode(), e);
//            return ResponseEntity.status(e.getStatusCode()).header("Content-Type", "text/html; charset=utf-8").body(e.getResponseBodyAsString());
//        }
    }

}
