package jebsen.ms.caiso.broker.casiobroker.services;

import com.fasterxml.jackson.databind.JsonNode;
import jebsen.ms.caiso.broker.casiobroker.config.AxMethodConfig;
import jebsen.ms.caiso.broker.casiobroker.entites.RequestMethod;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class AxOmsBrokerService extends BasicBrokerService {
    private final AxMethodConfig axMethodConfig;

    public ResponseEntity<String> handleRequest(JsonNode requestBodyJson, String method) throws Exception {
        return super.handleRequest(requestBodyJson, method, axMethodConfig);
    }

    @Override
    protected boolean checkResponseSuccess(ResponseEntity<String> responseEntity, RequestMethod requestMethod) {
        Document doc = parseXmlResponse(responseEntity.getBody());
        Element returnCode = (Element) doc.getElementsByTagName("return_code").item(0);
        return "0".equals(getCharacterDataFromElement(returnCode));
    }

    @Override
    protected String getResponseStr(ResponseEntity<String> responseEntity) {
        return responseEntity.getBody();
    }

    @Override
    protected ResponseEntity<String> submitRequest(JsonNode requestBodyJson) throws HttpStatusCodeException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        headers.set("Content-Type", "text/plain; charset=UTF-8");
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

        Map<String, String> queryParams = objectMapper.convertValue(requestBodyJson, HashMap.class);

        var paramString = new ArrayList<>(queryParams.entrySet()).stream()
                .map(m -> String.format("%s=%s", m.getKey(), StringUtils.isNotBlank(m.getValue()) ? URLEncoder.encode(m.getValue(), StandardCharsets.UTF_8) : ""))
                .collect(Collectors.joining("&"));

        log.info("paramString = {}", paramString);
        var finalUrl = String.format("%s?%s", axMethodConfig.getUrlPrefix(), paramString);
        log.info("Calling AX OMS: {}", finalUrl);

//        try {
            return new RestTemplate().exchange(URI.create(finalUrl), HttpMethod.GET, entity, String.class);
//        } catch (HttpStatusCodeException e) {
//            log.error("Calling AX OMS with error, status code :{}", e.getStatusCode(), e);
//            return ResponseEntity.status(e.getStatusCode()).header("Content-Type", "text/html; charset=utf-8").body(e.getResponseBodyAsString());
//        }
    }

    private Document parseXmlResponse(String xmlRes) {

        Document doc = null;
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlRes));
            doc = db.parse(is);
        } catch (Exception e) {
            log.error("Fail to parse xml response from AX OMS", e);
        }
        return doc;
    }

    public static String getCharacterDataFromElement(Element e) {
        if (ObjectUtils.isNotEmpty(e)) {
            Node child = e.getFirstChild();
            if (child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
        }
        return "";
    }
}
