package jebsen.ms.caiso.broker.casiobroker.entites;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRecordRepository extends JpaRepository<RequestRecord, Long> {
    Optional<RequestRecord> findFirstByKeyAndKeyValueAndMethod(String key, String keyValue, String method);
    List<RequestRecord> findBySuccessAndRetryMode(Boolean isSuccess,String retryMode);
    List<RequestRecord> findBySuccess(Boolean isSuccess);
}
