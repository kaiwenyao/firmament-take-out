package dev.kaiwen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev") // 2. 加上这一行，强制激活 dev 环境
class FirmamentServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
