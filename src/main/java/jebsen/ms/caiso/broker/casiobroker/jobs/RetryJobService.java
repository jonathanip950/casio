package jebsen.ms.caiso.broker.casiobroker.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jebsen.ms.caiso.broker.casiobroker.config.OracleConfig;
import jebsen.ms.caiso.broker.casiobroker.config.RetryJobConfig;
import jebsen.ms.caiso.broker.casiobroker.entites.RequestRecord;
import jebsen.ms.caiso.broker.casiobroker.entites.RequestRecordRepository;
import jebsen.ms.caiso.broker.casiobroker.services.AxOmsBrokerService;
import jebsen.ms.caiso.broker.casiobroker.services.OracleBrokerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Slf4j
public class RetryJobService {
    private final RequestRecordRepository requestRecordRepository;
    private final AxOmsBrokerService axOmsBrokerService;
    private final OracleBrokerService oracleBrokerService;
    private final ObjectMapper objectMapper;
    private final OracleConfig oracleConfig;

    public ResponseEntity<String> handleRetryRequest(String retryMode) {
        log.info("Batch retry started ,mode: {}", retryMode);
        CompletableFuture.supplyAsync(() -> batchRetry(retryMode)).thenAccept(s -> log.info("Batch retry completed with: {} ,mode: {}", s, retryMode)).exceptionally(e -> {
            log.error("{} occurs error", e);
            return null;
        });

        return ResponseEntity.ok("Batch retry is started");
    }

    private boolean batchRetry(String retryMode) {

        List<RequestRecord> unSuccessfulRecs = (RetryJobConfig.BatchRetryMode.ALL.toString().equals(retryMode))
                ? requestRecordRepository.findBySuccess(false)
                : requestRecordRepository.findBySuccessAndRetryMode(false, retryMode);
        unSuccessfulRecs.forEach(requestRecord -> {
                    try {
                        if (RetryJobConfig.Destination.AXOMS.toString().equals(requestRecord.getDestination())) {
                            axOmsBrokerService.handleRequest(objectMapper.readTree(requestRecord.getRequest()), requestRecord.getMethod());
                        } else if (RetryJobConfig.Destination.ORACLE.toString().equals(requestRecord.getDestination())) {
                            LocalDateTime currentDateTime = LocalDateTime.now(ZoneId.systemDefault().normalized());
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                            Boolean isOracleWorkingHours = oracleConfig.isOracleWorkingHours(currentDateTime);
                            log.info("Oracle Retry : Is within Oracle offline hours ?: {} ,current time: {}", isOracleWorkingHours, currentDateTime.format(formatter));
                            if(isOracleWorkingHours) {
                                oracleBrokerService.handleRequest(objectMapper.readTree(requestRecord.getRequest()), requestRecord.getMethod());
                            }
                        }
                    } catch (IOException e) {
                        log.error("[RetryJob] Fail to parse record from JSON to Map", e);
                    } catch (Exception e) {
                        log.error("[RetryJob] Fail to try request : {} ", requestRecord.toString(), e);
                    }
                }
        );
        return true;
    }
}
