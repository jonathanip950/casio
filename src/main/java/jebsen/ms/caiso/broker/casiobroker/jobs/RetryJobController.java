package jebsen.ms.caiso.broker.casiobroker.jobs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("job/")
@Slf4j
@AllArgsConstructor
public class RetryJobController {
    private final RetryJobService retryJobService;

    @GetMapping("retry")
    public ResponseEntity<String> handleRetryAxRequest(@RequestHeader("RETRY_MODE") String retryMode) {
        return retryJobService.handleRetryRequest(retryMode);
    }
}
