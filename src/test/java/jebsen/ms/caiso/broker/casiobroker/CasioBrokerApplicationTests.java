package jebsen.ms.caiso.broker.casiobroker;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;


@SpringBootApplication
@Slf4j
//@ComponentScan({"jebsen.ms.test", "jebsen.ms.crm.broker"})
public class CasioBrokerApplicationTests {

    public static void main(String[] args) {
        SpringApplication.run(CasioBrokerApplicationTests.class, args);
    }

    @SneakyThrows
    public void other() {
        test();
    }

    @PostConstruct
    public void test() throws Exception {
    }
}

//@SpringBootTest
//class CasioBrokerApplicationTests {
//
//    @Test
//    void contextLoads() {
//    }
//
//}
