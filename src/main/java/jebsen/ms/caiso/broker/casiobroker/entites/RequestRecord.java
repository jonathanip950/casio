package jebsen.ms.caiso.broker.casiobroker.entites;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Component
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "request_record")
public class RequestRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column
    private String key;

    @Column
    private String keyValue;

    @Column
    private String method;

    @Column(nullable = false, columnDefinition="TEXT")
    private String request;

    @Column
    private Boolean success;

    @Column(columnDefinition="TEXT")
    private String response;

    @Column
    private String retryMode;

    @Column
    private String destination;

    @CreationTimestamp
    protected LocalDateTime createdOn;

    @UpdateTimestamp
    protected LocalDateTime updatedOn;

}
