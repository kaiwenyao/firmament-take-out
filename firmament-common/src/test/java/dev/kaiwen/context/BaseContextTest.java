package dev.kaiwen.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BaseContextTest {

  // Ensure each test leaves ThreadLocal clean for isolation.
  @AfterEach
  void tearDown() {
    BaseContext.removeCurrentId();
  }

  @Test
  // Basic ThreadLocal lifecycle in a single thread.
  void setGetRemoveWorksInSameThread() {
    assertNull(BaseContext.getCurrentId());

    BaseContext.setCurrentId(42L);
    assertEquals(42L, BaseContext.getCurrentId());

    BaseContext.removeCurrentId();
    assertNull(BaseContext.getCurrentId());
  }

  @Test
  // Verify that values do not leak across threads.
  void threadLocalIsIsolatedBetweenThreads() throws Exception {
    BaseContext.setCurrentId(1L);

    AtomicReference<Long> otherThreadValue = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Thread thread = new Thread(() -> {
      otherThreadValue.set(BaseContext.getCurrentId());
      latch.countDown();
    });

    thread.start();
    assertTrue(latch.await(2, TimeUnit.SECONDS));

    assertNull(otherThreadValue.get());
    assertEquals(1L, BaseContext.getCurrentId());
  }

  @Test
  // Explicit remove should clear the stored value.
  void removeClearsValue() {
    BaseContext.setCurrentId(7L);
    BaseContext.removeCurrentId();
    assertNull(BaseContext.getCurrentId());
  }
}
