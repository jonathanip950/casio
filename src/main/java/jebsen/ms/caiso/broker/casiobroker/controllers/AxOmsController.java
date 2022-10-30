package jebsen.ms.caiso.broker.casiobroker.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jebsen.ms.caiso.broker.casiobroker.services.AxOmsBrokerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("JEB_JCP/api/oms/CSOVHK")
@Slf4j
@AllArgsConstructor
public class AxOmsController {

    private final AxOmsBrokerService axOmsBrokerService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public ResponseEntity<String> handleAxRequest(@RequestParam() Map<String, String> params) throws Exception {
        return axOmsBrokerService.handleRequest(objectMapper.readTree(objectMapper.writeValueAsString(params)), params.get("method"));
    }
}
