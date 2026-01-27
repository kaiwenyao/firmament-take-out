package dev.kaiwen.task;

import static org.mockito.Mockito.verify;

import dev.kaiwen.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTaskTest {

  @Mock
  private OrderService orderService;

  private OrderTask orderTask;

  @BeforeEach
  void setUp() {
    orderTask = new OrderTask(orderService);
  }

  @Test
  void processTimeoutOrderInvokesService() {
    orderTask.processTimeoutOrder();
    verify(orderService).processTimeoutOrder();
  }

  @Test
  void processDeliveryOrderInvokesService() {
    orderTask.processDeliveryOrder();
    verify(orderService).processDeliveryOrder();
  }
}
