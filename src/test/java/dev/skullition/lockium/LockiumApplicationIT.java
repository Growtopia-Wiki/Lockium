package dev.skullition.lockium;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Live application smoke test.
 *
 * <p>The {@code IT} suffix keeps this test out of Maven Surefire's default unit-test patterns. Run
 * it explicitly with {@code mvnw.cmd test -Dtest=LockiumApplicationIT}; valid Discord and Wiki
 * credentials are required because the complete production context connects to both services.
 */
@SpringBootTest
class LockiumApplicationIT {

  @Test
  void contextLoads() {}
}
