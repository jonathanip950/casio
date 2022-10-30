package jebsen.ms.caiso.broker.casiobroker.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jebsen.ms.caiso.broker.casiobroker.services.OracleBrokerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("webservices/rest/CSHKSHOP_OM_PKG")
@Slf4j
@AllArgsConstructor
public class OracleController {

    private final OracleBrokerService oracleBrokerService;
    private final ObjectMapper objectMapper;

    @PostMapping("/{oracleMethod}")
    public ResponseEntity<String> handleOracleRequest(@PathVariable String oracleMethod ,@RequestBody String request) throws Exception {
        return oracleBrokerService.handleRequest(objectMapper.readTree(request),oracleMethod);
    }
}
