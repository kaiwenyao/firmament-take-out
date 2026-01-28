package dev.kaiwen.service.impl;

import static dev.kaiwen.constant.MessageConstant.ORDER_NOT_FOUND;
import static dev.kaiwen.constant.MessageConstant.ORDER_STATUS_ERROR;
import static dev.kaiwen.constant.MessageConstant.SHOPPING_CART_IS_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.dto.OrdersCancelDto;
import dev.kaiwen.dto.OrdersConfirmDto;
import dev.kaiwen.dto.OrdersPageQueryDto;
import dev.kaiwen.dto.OrdersPaymentDto;
import dev.kaiwen.dto.OrdersRejectionDto;
import dev.kaiwen.dto.OrdersSubmitDto;
import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.entity.User;
import dev.kaiwen.exception.OrderBusinessException;
import dev.kaiwen.exception.ShoppingCartBusinessException;
import dev.kaiwen.mapper.OrderDetailMapper;
import dev.kaiwen.mapper.OrderMapper;
import dev.kaiwen.mapper.ShoppingCartMapper;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.AddressBookService;
import dev.kaiwen.service.OrderDetailService;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.service.ShoppingCartService;
import dev.kaiwen.service.UserService;
import dev.kaiwen.vo.OrderStatisticsVo;
import dev.kaiwen.vo.OrderSubmitVo;
import dev.kaiwen.vo.OrderVo;
import dev.kaiwen.websocket.WebSocketServer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @InjectMocks
  private OrderServiceImpl orderService;

  @Mock
  private OrderMapper mapper;

  @Mock
  private ShoppingCartMapper shoppingCartMapper;

  @Mock
  private OrderDetailMapper orderDetailMapper;

  @Mock
  private OrderDetailService orderDetailService;

  @Mock
  private AddressBookService addressBookService;

  @Mock
  private ShoppingCartService shoppingCartService;

  @Mock
  private UserService userService;

  @Mock
  private WebSocketServer webSocketServer;

  @Mock
  private ObjectProvider<OrderService> orderServiceProvider;

  @Mock
  private OrderService orderServiceProxy;

  @Captor
  private ArgumentCaptor<Orders> ordersCaptor;

  @Captor
  private ArgumentCaptor<String> messageCaptor;

  @Captor
  private ArgumentCaptor<List<Orders>> ordersListCaptor;

  @Captor
  private ArgumentCaptor<List<OrderDetail>> orderDetailCaptor;

  @Captor
  private ArgumentCaptor<List<ShoppingCart>> shoppingCartCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, Orders.class);
    TableInfoHelper.initTableInfo(assistant, OrderDetail.class);
    TableInfoHelper.initTableInfo(assistant, ShoppingCart.class);
    ReflectionTestUtils.setField(orderService, "baseMapper", mapper);
  }

  @Test
  void submitOrderSuccessCalculatesAmountAndSavesDetails() {
    OrdersSubmitDto dto = new OrdersSubmitDto();
    dto.setAddressBookId(1L);
    dto.setPackAmount(3);
    dto.setAmount(new BigDecimal("999"));

    ShoppingCart cart1 = new ShoppingCart();
    cart1.setAmount(new BigDecimal("10"));
    cart1.setNumber(2);
    ShoppingCart cart2 = new ShoppingCart();
    cart2.setAmount(new BigDecimal("5"));
    cart2.setNumber(1);

    when(shoppingCartMapper.selectCount(any())).thenReturn(1L);
    when(shoppingCartService.showShoppingCart()).thenReturn(List.of(cart1, cart2));

    AddressBook addressBook = new AddressBook();
    addressBook.setConsignee("Tom");
    addressBook.setPhone("123");
    addressBook.setProvinceName("A");
    addressBook.setCityName(null);
    addressBook.setDistrictName("B");
    addressBook.setDetail("C");
    when(addressBookService.getByIdWithCheck(1L)).thenReturn(addressBook);

    User user = new User();
    user.setName("Jerry");
    when(userService.getById(9001L)).thenReturn(user);

    doAnswer(invocation -> {
      Orders orders = invocation.getArgument(0);
      orders.setId(100L);
      return 1;
    }).when(mapper).insert(any(Orders.class));

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(9001L);

      OrderSubmitVo result = orderService.submitOrder(dto);

      assertEquals(100L, result.getId());
      assertEquals(new BigDecimal("28"), result.getOrderAmount());
      assertNotNull(result.getOrderNumber());
      assertNotNull(result.getOrderTime());
    }

    verify(mapper).insert(ordersCaptor.capture());
    Orders savedOrder = ordersCaptor.getValue();
    assertEquals(new BigDecimal("28"), savedOrder.getAmount());
    assertEquals("ABC", savedOrder.getAddress());
    assertEquals("Tom", savedOrder.getConsignee());
    assertEquals("123", savedOrder.getPhone());
    assertEquals("Jerry", savedOrder.getUserName());
    assertEquals(Orders.PENDING_PAYMENT, savedOrder.getStatus());

    verify(orderDetailService).saveBatch(orderDetailCaptor.capture());
    List<OrderDetail> savedDetails = orderDetailCaptor.getValue();
    assertEquals(2, savedDetails.size());
    assertTrue(savedDetails.stream().allMatch(detail -> 100L == detail.getOrderId()));
    verify(shoppingCartService).cleanShoppingCart();
  }

  @Test
  void submitOrderThrowsWhenCartCountZero() {
    OrdersSubmitDto dto = new OrdersSubmitDto();
    dto.setAddressBookId(1L);

    when(shoppingCartMapper.selectCount(any())).thenReturn(0L);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(9002L);

      ShoppingCartBusinessException exception = assertThrows(ShoppingCartBusinessException.class,
          () -> orderService.submitOrder(dto));

      assertEquals(SHOPPING_CART_IS_NULL, exception.getMessage());
    }
  }

  @Test
  void submitOrderThrowsWhenCartListEmpty() {
    OrdersSubmitDto dto = new OrdersSubmitDto();
    dto.setAddressBookId(1L);

    when(shoppingCartMapper.selectCount(any())).thenReturn(1L);
    when(shoppingCartService.showShoppingCart()).thenReturn(Collections.emptyList());

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(9003L);

      ShoppingCartBusinessException exception = assertThrows(ShoppingCartBusinessException.class,
          () -> orderService.submitOrder(dto));

      assertEquals(SHOPPING_CART_IS_NULL, exception.getMessage());
    }
  }

  @Test
  void submitOrderIgnoresNullCartFieldsAndNoPackAmount() {
    OrdersSubmitDto dto = new OrdersSubmitDto();
    dto.setAddressBookId(2L);
    dto.setPackAmount(0);

    ShoppingCart cart1 = new ShoppingCart();
    cart1.setAmount(null);
    cart1.setNumber(2);
    ShoppingCart cart2 = new ShoppingCart();
    cart2.setAmount(new BigDecimal("10"));
    cart2.setNumber(null);
    ShoppingCart cart3 = new ShoppingCart();
    cart3.setAmount(new BigDecimal("2"));
    cart3.setNumber(3);

    when(shoppingCartMapper.selectCount(any())).thenReturn(1L);
    when(shoppingCartService.showShoppingCart()).thenReturn(List.of(cart1, cart2, cart3));

    AddressBook addressBook = new AddressBook();
    addressBook.setConsignee(null);
    addressBook.setPhone(null);
    addressBook.setProvinceName(null);
    addressBook.setCityName(null);
    addressBook.setDistrictName(null);
    addressBook.setDetail(null);
    when(addressBookService.getByIdWithCheck(2L)).thenReturn(addressBook);

    User user = new User();
    user.setName(null);
    when(userService.getById(12L)).thenReturn(user);

    doAnswer(invocation -> {
      Orders orders = invocation.getArgument(0);
      orders.setId(101L);
      return 1;
    }).when(mapper).insert(any(Orders.class));

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(12L);

      OrderSubmitVo result = orderService.submitOrder(dto);

      assertEquals(101L, result.getId());
      assertEquals(new BigDecimal("6"), result.getOrderAmount());
      assertEquals(24, result.getOrderNumber().length());
      assertEquals("0012", result.getOrderNumber().substring(17, 21));
    }

    verify(mapper).insert(ordersCaptor.capture());
    Orders savedOrder = ordersCaptor.getValue();
    assertEquals(new BigDecimal("6"), savedOrder.getAmount());
    assertEquals("", savedOrder.getAddress());
    assertNull(savedOrder.getUserName());
  }

  @Test
  void submitOrderFallsBackWhenOrderNumberCollides() {
    OrdersSubmitDto dto = new OrdersSubmitDto();
    dto.setAddressBookId(3L);
    dto.setPackAmount(null);

    ShoppingCart cart = new ShoppingCart();
    cart.setAmount(new BigDecimal("1"));
    cart.setNumber(1);

    when(shoppingCartMapper.selectCount(any())).thenReturn(1L);
    when(shoppingCartService.showShoppingCart()).thenReturn(List.of(cart));

    AddressBook addressBook = new AddressBook();
    addressBook.setConsignee("A");
    addressBook.setPhone("B");
    addressBook.setProvinceName("C");
    addressBook.setCityName("D");
    addressBook.setDistrictName("E");
    addressBook.setDetail("F");
    when(addressBookService.getByIdWithCheck(3L)).thenReturn(addressBook);

    when(mapper.selectCount(any())).thenReturn(1L, 1L, 1L, 1L, 1L);

    doAnswer(invocation -> {
      Orders orders = invocation.getArgument(0);
      orders.setId(102L);
      return 1;
    }).when(mapper).insert(any(Orders.class));

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(12345L);

      OrderSubmitVo result = orderService.submitOrder(dto);

      assertEquals(102L, result.getId());
      assertEquals(27, result.getOrderNumber().length());
    }
  }

  @Test
  void detailsNotFoundThrows() {
    when(mapper.selectById(1L)).thenReturn(null);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.details(1L));

    assertEquals(ORDER_NOT_FOUND, exception.getMessage());
  }

  @Test
  void detailsSuccess() {
    Orders orders = new Orders();
    orders.setId(2L);
    orders.setNumber("N1");

    OrderDetail detail = new OrderDetail();
    detail.setOrderId(2L);

    when(mapper.selectById(2L)).thenReturn(orders);
    when(orderDetailMapper.selectList(any())).thenReturn(List.of(detail));

    OrderVo result = orderService.details(2L);

    assertNotNull(result);
    assertEquals(1, result.getOrderDetailList().size());
  }

  @Test
  void detailsByNumberSuccess() {
    Orders orders = new Orders();
    orders.setId(3L);
    orders.setNumber("N2");
    orders.setUserId(100L);

    OrderDetail detail = new OrderDetail();
    detail.setOrderId(3L);

    when(mapper.selectOne(any())).thenReturn(orders);
    when(orderDetailMapper.selectList(any())).thenReturn(List.of(detail));

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(100L);

      OrderVo result = orderService.detailsByNumber("N2");

      assertNotNull(result);
      assertEquals(1, result.getOrderDetailList().size());
    }
  }

  @Test
  void detailsByNumberNotFoundThrows() {
    when(mapper.selectOne(any())).thenReturn(null);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(100L);

      OrderBusinessException exception = assertThrows(OrderBusinessException.class,
          () -> orderService.detailsByNumber("N404"));

      assertEquals(ORDER_NOT_FOUND, exception.getMessage());
    }
  }

  @Test
  void detailsByNumberWithEmptyNumberThrows() {
    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.detailsByNumber(""));

    assertEquals(ORDER_NOT_FOUND, exception.getMessage());
  }

  @Test
  void pageQuery4UserWithRecords() {
    Orders orders = new Orders();
    orders.setId(5L);

    OrderDetail detail = new OrderDetail();
    detail.setOrderId(5L);

    doAnswer(invocation -> {
      Page<Orders> pageArg = invocation.getArgument(0);
      pageArg.setRecords(List.of(orders));
      pageArg.setTotal(1L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    when(orderDetailMapper.selectList(any())).thenReturn(List.of(detail));

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(101L);

      PageResult result = orderService.pageQuery4User(1, 10, null);

      assertEquals(1L, result.getTotal());
      @SuppressWarnings("unchecked")
      List<OrderVo> recordList = (List<OrderVo>) result.getRecords();
      assertEquals(1, recordList.size());
      assertEquals(1, recordList.get(0).getOrderDetailList().size());
    }
  }

  @Test
  void pageQuery4UserWithNoRecordsReturnsEmpty() {
    doAnswer(invocation -> {
      Page<Orders> pageArg = invocation.getArgument(0);
      pageArg.setRecords(Collections.emptyList());
      pageArg.setTotal(0L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(101L);

      PageResult result = orderService.pageQuery4User(1, 10, 1);

      assertEquals(0L, result.getTotal());
    }
  }

  @Test
  void paymentSuccess() {
    Orders orders = new Orders();
    orders.setId(6L);
    orders.setNumber("N6");
    orders.setStatus(Orders.PENDING_PAYMENT);
    orders.setPayStatus(Orders.UN_PAID);

    OrdersPaymentDto dto = new OrdersPaymentDto();
    dto.setOrderNumber("N6");
    dto.setPayMethod(1);

    when(mapper.selectOne(any())).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(200L);

      orderService.payment(dto);

      verify(mapper).updateById(ordersCaptor.capture());
      Orders saved = ordersCaptor.getValue();
      assertEquals(Orders.PAID, saved.getPayStatus());
      assertEquals(Orders.TO_BE_CONFIRMED, saved.getStatus());
      assertNotNull(saved.getCheckoutTime());
      assertEquals(1, saved.getPayMethod());
      verify(webSocketServer).sendToAllClient(messageCaptor.capture());
    }
  }

  @Test
  void paymentWithoutPayMethodKeepsExisting() {
    Orders orders = new Orders();
    orders.setId(6L);
    orders.setNumber("N6");
    orders.setStatus(Orders.PENDING_PAYMENT);
    orders.setPayStatus(Orders.UN_PAID);
    orders.setPayMethod(2);

    OrdersPaymentDto dto = new OrdersPaymentDto();
    dto.setOrderNumber("N6");
    dto.setPayMethod(null);

    when(mapper.selectOne(any())).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(200L);

      orderService.payment(dto);

      verify(mapper).updateById(ordersCaptor.capture());
      Orders saved = ordersCaptor.getValue();
      assertEquals(2, saved.getPayMethod());
    }
  }

  @Test
  void paymentOrderNotFoundThrows() {
    OrdersPaymentDto dto = new OrdersPaymentDto();
    dto.setOrderNumber("N7");

    when(mapper.selectOne(any())).thenReturn(null);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(201L);

      OrderBusinessException exception = assertThrows(OrderBusinessException.class,
          () -> orderService.payment(dto));

      assertEquals(ORDER_NOT_FOUND, exception.getMessage());
    }
  }

  @Test
  void paymentWrongStatusThrows() {
    Orders orders = new Orders();
    orders.setStatus(Orders.CANCELLED);
    orders.setPayStatus(Orders.UN_PAID);

    OrdersPaymentDto dto = new OrdersPaymentDto();
    dto.setOrderNumber("N8");

    when(mapper.selectOne(any())).thenReturn(orders);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(202L);

      OrderBusinessException exception = assertThrows(OrderBusinessException.class,
          () -> orderService.payment(dto));

      assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
    }
  }

  @Test
  void paymentAlreadyPaidThrows() {
    Orders orders = new Orders();
    orders.setStatus(Orders.PENDING_PAYMENT);
    orders.setPayStatus(Orders.PAID);

    OrdersPaymentDto dto = new OrdersPaymentDto();
    dto.setOrderNumber("N9");

    when(mapper.selectOne(any())).thenReturn(orders);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(203L);

      OrderBusinessException exception = assertThrows(OrderBusinessException.class,
          () -> orderService.payment(dto));

      assertEquals("订单已支付，请勿重复支付", exception.getMessage());
    }
  }

  @Test
  void processTimeoutOrderUpdatesBatch() {
    Orders order = new Orders();
    order.setId(10L);
    order.setStatus(Orders.PENDING_PAYMENT);
    order.setPayStatus(Orders.UN_PAID);
    order.setOrderTime(LocalDateTime.now().minusMinutes(20));

    when(orderServiceProvider.getObject()).thenReturn(orderServiceProxy);
    when(mapper.selectList(any())).thenReturn(List.of(order));

    orderService.processTimeoutOrder();

    verify(orderServiceProxy).updateBatchById(ordersListCaptor.capture());
    List<Orders> updated = ordersListCaptor.getValue();
    assertEquals(Orders.CANCELLED, updated.get(0).getStatus());
    assertNotNull(updated.get(0).getCancelTime());
  }

  @Test
  void processTimeoutOrderSkipsWhenEmpty() {
    when(mapper.selectList(any())).thenReturn(List.of());

    orderService.processTimeoutOrder();

    verify(orderServiceProxy, never()).updateBatchById(any());
  }

  @Test
  void processTimeoutOrderSkipsWhenNull() {
    when(mapper.selectList(any())).thenReturn(null);

    orderService.processTimeoutOrder();

    verify(orderServiceProxy, never()).updateBatchById(any());
  }

  @Test
  void processDeliveryOrderUpdatesBatch() {
    Orders order = new Orders();
    order.setId(11L);
    order.setStatus(Orders.DELIVERY_IN_PROGRESS);
    order.setOrderTime(LocalDateTime.now().minusDays(1));

    when(orderServiceProvider.getObject()).thenReturn(orderServiceProxy);
    when(mapper.selectList(any())).thenReturn(List.of(order));

    orderService.processDeliveryOrder();

    verify(orderServiceProxy).updateBatchById(ordersListCaptor.capture());
    List<Orders> updated = ordersListCaptor.getValue();
    assertEquals(Orders.COMPLETED, updated.get(0).getStatus());
    assertNotNull(updated.get(0).getDeliveryTime());
  }

  @Test
  void processDeliveryOrderSkipsWhenEmpty() {
    when(mapper.selectList(any())).thenReturn(Collections.emptyList());

    orderService.processDeliveryOrder();

    verify(orderServiceProxy, never()).updateBatchById(any());
  }

  @Test
  void processDeliveryOrderSkipsWhenNull() {
    when(mapper.selectList(any())).thenReturn(null);

    orderService.processDeliveryOrder();

    verify(orderServiceProxy, never()).updateBatchById(any());
  }

  @Test
  void processDeliveryOrderKeepsExistingDeliveryTime() {
    Orders order = new Orders();
    order.setId(12L);
    order.setStatus(Orders.DELIVERY_IN_PROGRESS);
    order.setOrderTime(LocalDateTime.now().minusDays(1));
    LocalDateTime deliveredAt = LocalDateTime.now().minusHours(2);
    order.setDeliveryTime(deliveredAt);

    when(orderServiceProvider.getObject()).thenReturn(orderServiceProxy);
    when(mapper.selectList(any())).thenReturn(List.of(order));

    orderService.processDeliveryOrder();

    verify(orderServiceProxy).updateBatchById(ordersListCaptor.capture());
    List<Orders> updated = ordersListCaptor.getValue();
    assertEquals(Orders.COMPLETED, updated.get(0).getStatus());
    assertEquals(deliveredAt, updated.get(0).getDeliveryTime());
  }

  @Test
  void conditionSearchBuildsOrderDishes() {
    OrdersPageQueryDto dto = new OrdersPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);
    dto.setNumber("N20");
    dto.setPhone("123");
    dto.setUserId(20L);
    dto.setStatus(Orders.CONFIRMED);
    dto.setBeginTime(LocalDateTime.now().minusDays(1));
    dto.setEndTime(LocalDateTime.now());

    Orders ordersRecord = new Orders();
    ordersRecord.setId(20L);
    ordersRecord.setNumber("N20");

    doAnswer(invocation -> {
      Page<Orders> pageArg = invocation.getArgument(0);
      pageArg.setRecords(List.of(ordersRecord));
      pageArg.setTotal(1L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    OrderDetail detail = new OrderDetail();
    detail.setName("宫保鸡丁");
    detail.setNumber(2);
    when(orderDetailMapper.selectList(any())).thenReturn(List.of(detail));

    PageResult result = orderService.conditionSearch(dto);

    assertEquals(1L, result.getTotal());
    @SuppressWarnings("unchecked")
    List<OrderVo> voList = (List<OrderVo>) result.getRecords();
    assertEquals("宫保鸡丁*2;", voList.get(0).getOrderDishes());
  }

  @Test
  void conditionSearchWithEmptyFiltersReturnsEmpty() {
    OrdersPageQueryDto dto = new OrdersPageQueryDto();
    dto.setPage(1);
    dto.setPageSize(10);

    doAnswer(invocation -> {
      Page<Orders> pageArg = invocation.getArgument(0);
      pageArg.setRecords(Collections.emptyList());
      pageArg.setTotal(0L);
      return pageArg;
    }).when(mapper).selectPage(any(), any());

    PageResult result = orderService.conditionSearch(dto);

    assertEquals(0L, result.getTotal());
    @SuppressWarnings("unchecked")
    List<OrderVo> voList = (List<OrderVo>) result.getRecords();
    assertTrue(voList.isEmpty());
    verify(orderDetailMapper, never()).selectList(any());
  }

  @Test
  void statisticsReturnsCounts() {
    when(mapper.selectCount(any())).thenReturn(1L, 2L, 3L);

    OrderStatisticsVo statistics = orderService.statistics();

    assertEquals(1, statistics.getToBeConfirmed());
    assertEquals(2, statistics.getConfirmed());
    assertEquals(3, statistics.getDeliveryInProgress());
  }

  @Test
  void userCancelByIdStatusErrorThrows() {
    Orders orders = new Orders();
    orders.setId(12L);
    orders.setStatus(Orders.CONFIRMED);

    when(mapper.selectById(12L)).thenReturn(orders);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.userCancelById(12L));

    assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
  }

  @Test
  void userCancelByIdNotFoundThrows() {
    when(mapper.selectById(30L)).thenReturn(null);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.userCancelById(30L));

    assertEquals(ORDER_NOT_FOUND, exception.getMessage());
  }

  @Test
  void userCancelByIdRefundsWhenToBeConfirmed() {
    Orders orders = new Orders();
    orders.setId(13L);
    orders.setNumber("N13");
    orders.setStatus(Orders.TO_BE_CONFIRMED);
    orders.setPayStatus(Orders.PAID);

    when(mapper.selectById(13L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    orderService.userCancelById(13L);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.CANCELLED, updated.getStatus());
    assertEquals("用户取消", updated.getCancelReason());
    assertNotNull(updated.getCancelTime());
    assertEquals(Orders.REFUND, updated.getPayStatus());
  }

  @Test
  void userCancelByIdWithoutRefundWhenPendingPayment() {
    Orders orders = new Orders();
    orders.setId(29L);
    orders.setStatus(Orders.PENDING_PAYMENT);
    orders.setPayStatus(Orders.UN_PAID);

    when(mapper.selectById(29L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    orderService.userCancelById(29L);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.CANCELLED, updated.getStatus());
    assertEquals("用户取消", updated.getCancelReason());
    assertNotNull(updated.getCancelTime());
    assertNull(updated.getPayStatus());
  }

  @Test
  void userCancelByNumberDelegates() {
    Orders orders = new Orders();
    orders.setId(14L);
    orders.setNumber("N14");
    orders.setUserId(300L);

    when(orderServiceProvider.getObject()).thenReturn(orderServiceProxy);
    when(mapper.selectOne(any())).thenReturn(orders);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(300L);

      orderService.userCancelByNumber("N14");

      verify(orderServiceProxy).userCancelById(14L);
    }
  }

  @Test
  void confirmUpdatesStatus() {
    OrdersConfirmDto dto = new OrdersConfirmDto();
    dto.setId(15L);

    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    orderService.confirm(dto);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.CONFIRMED, updated.getStatus());
  }

  @Test
  void rejectionThrowsWhenStatusInvalid() {
    Orders orders = new Orders();
    orders.setId(16L);
    orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

    when(mapper.selectById(16L)).thenReturn(orders);

    OrdersRejectionDto dto = new OrdersRejectionDto();
    dto.setId(16L);
    dto.setRejectionReason("满单");

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.rejection(dto));

    assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
  }

  @Test
  void rejectionThrowsWhenOrderMissing() {
    when(mapper.selectById(16L)).thenReturn(null);

    OrdersRejectionDto dto = new OrdersRejectionDto();
    dto.setId(16L);
    dto.setRejectionReason("满单");

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.rejection(dto));

    assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
  }

  @Test
  void rejectionWithoutRefundWhenUnpaid() {
    Orders orders = new Orders();
    orders.setId(26L);
    orders.setStatus(Orders.TO_BE_CONFIRMED);
    orders.setPayStatus(Orders.UN_PAID);

    when(mapper.selectById(26L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    OrdersRejectionDto dto = new OrdersRejectionDto();
    dto.setId(26L);
    dto.setRejectionReason("售罄");

    orderService.rejection(dto);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.CANCELLED, updated.getStatus());
    assertEquals("售罄", updated.getRejectionReason());
    assertNotNull(updated.getCancelTime());
    assertNull(updated.getPayStatus());
  }

  @Test
  void rejectionRefundsWhenPaid() {
    Orders orders = new Orders();
    orders.setId(17L);
    orders.setNumber("N17");
    orders.setStatus(Orders.TO_BE_CONFIRMED);
    orders.setPayStatus(Orders.PAID);

    when(mapper.selectById(17L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    OrdersRejectionDto dto = new OrdersRejectionDto();
    dto.setId(17L);
    dto.setRejectionReason("打烊");

    orderService.rejection(dto);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.CANCELLED, updated.getStatus());
    assertEquals("打烊", updated.getRejectionReason());
    assertEquals(Orders.REFUND, updated.getPayStatus());
  }

  @Test
  void cancelRefundsWhenPaid() {
    Orders orders = new Orders();
    orders.setId(18L);
    orders.setNumber("N18");
    orders.setPayStatus(Orders.PAID);

    when(mapper.selectById(18L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    OrdersCancelDto dto = new OrdersCancelDto();
    dto.setId(18L);
    dto.setCancelReason("超时");

    orderService.cancel(dto);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.CANCELLED, updated.getStatus());
    assertEquals("超时", updated.getCancelReason());
    assertEquals(Orders.REFUND, updated.getPayStatus());
  }

  @Test
  void cancelThrowsWhenOrderMissing() {
    when(mapper.selectById(27L)).thenReturn(null);

    OrdersCancelDto dto = new OrdersCancelDto();
    dto.setId(27L);
    dto.setCancelReason("关闭");

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.cancel(dto));

    assertEquals(ORDER_NOT_FOUND, exception.getMessage());
  }

  @Test
  void cancelWithoutRefundWhenUnpaid() {
    Orders orders = new Orders();
    orders.setId(28L);
    orders.setPayStatus(Orders.UN_PAID);

    when(mapper.selectById(28L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    OrdersCancelDto dto = new OrdersCancelDto();
    dto.setId(28L);
    dto.setCancelReason("取消");

    orderService.cancel(dto);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.CANCELLED, updated.getStatus());
    assertEquals("取消", updated.getCancelReason());
    assertNotNull(updated.getCancelTime());
    assertNull(updated.getPayStatus());
  }

  @Test
  void deliveryUpdatesStatus() {
    Orders orders = new Orders();
    orders.setId(19L);
    orders.setStatus(Orders.CONFIRMED);

    when(mapper.selectById(19L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    orderService.delivery(19L);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.DELIVERY_IN_PROGRESS, updated.getStatus());
  }

  @Test
  void deliveryThrowsWhenStatusInvalid() {
    Orders orders = new Orders();
    orders.setId(20L);
    orders.setStatus(Orders.PENDING_PAYMENT);

    when(mapper.selectById(20L)).thenReturn(orders);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.delivery(20L));

    assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
  }

  @Test
  void deliveryThrowsWhenOrderMissing() {
    when(mapper.selectById(31L)).thenReturn(null);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.delivery(31L));

    assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
  }

  @Test
  void completeUpdatesStatus() {
    Orders orders = new Orders();
    orders.setId(21L);
    orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

    when(mapper.selectById(21L)).thenReturn(orders);
    when(mapper.updateById(any(Orders.class))).thenReturn(1);

    orderService.complete(21L);

    verify(mapper).updateById(ordersCaptor.capture());
    Orders updated = ordersCaptor.getValue();
    assertEquals(Orders.COMPLETED, updated.getStatus());
    assertNotNull(updated.getDeliveryTime());
  }

  @Test
  void completeThrowsWhenStatusInvalid() {
    Orders orders = new Orders();
    orders.setId(22L);
    orders.setStatus(Orders.CONFIRMED);

    when(mapper.selectById(22L)).thenReturn(orders);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.complete(22L));

    assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
  }

  @Test
  void completeThrowsWhenOrderMissing() {
    when(mapper.selectById(32L)).thenReturn(null);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.complete(32L));

    assertEquals(ORDER_STATUS_ERROR, exception.getMessage());
  }

  @Test
  void reminderSendsMessage() {
    Orders orders = new Orders();
    orders.setId(22L);
    orders.setNumber("N22");

    when(mapper.selectById(22L)).thenReturn(orders);

    orderService.reminder(22L);

    verify(webSocketServer).sendToAllClient(messageCaptor.capture());
  }

  @Test
  void reminderThrowsWhenOrderMissing() {
    when(mapper.selectById(23L)).thenReturn(null);

    OrderBusinessException exception = assertThrows(OrderBusinessException.class,
        () -> orderService.reminder(23L));

    assertEquals(ORDER_NOT_FOUND, exception.getMessage());
  }

  @Test
  void repetitionByNumberDelegates() {
    Orders orders = new Orders();
    orders.setId(24L);
    orders.setNumber("N24");
    orders.setUserId(400L);

    when(orderServiceProvider.getObject()).thenReturn(orderServiceProxy);
    when(mapper.selectOne(any())).thenReturn(orders);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(400L);

      orderService.repetitionByNumber("N24");

      verify(orderServiceProxy).repetition(24L);
    }
  }

  @Test
  void reminderByNumberDelegatesToReminder() {
    Orders orders = new Orders();
    orders.setId(25L);
    orders.setNumber("N25");
    orders.setUserId(500L);

    when(mapper.selectOne(any())).thenReturn(orders);
    when(mapper.selectById(25L)).thenReturn(orders);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(500L);

      orderService.reminderByNumber("N25");

      verify(webSocketServer).sendToAllClient(messageCaptor.capture());
    }
  }

  @Test
  void repetitionCopiesOrderDetails() {
    OrderDetail detail = new OrderDetail();
    detail.setOrderId(20L);
    detail.setDishId(1L);
    detail.setName("菜品");
    detail.setNumber(2);

    when(orderDetailMapper.selectList(any())).thenReturn(List.of(detail));

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(500L);

      orderService.repetition(20L);

      verify(shoppingCartService).saveBatch(shoppingCartCaptor.capture());
      List<ShoppingCart> carts = shoppingCartCaptor.getValue();
      assertEquals(1, carts.size());
      assertEquals(500L, carts.get(0).getUserId());
    }
  }
}
