package dev.kaiwen.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.OrderConverter;
import dev.kaiwen.converter.OrderDetailConverter;
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
import dev.kaiwen.mapper.OrderMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 订单服务实现类.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

  private static final String ORDER_NUMBER_DATE_PATTERN = "yyyyMMddHHmmssSSS";
  private static final String REFUND_LOG_MESSAGE = "订单 {} 已退款（模拟）";
  private static final int ORDER_NUMBER_RANDOM_BOUND = 1000;

  private final OrderDetailService orderDetailService;
  private final AddressBookService addressBookService;
  private final ShoppingCartService shoppingCartService;
  private final UserService userService;
  private final WebSocketServer webSocketServer;
  private final ObjectProvider<OrderService> orderServiceProvider;

  /**
   * 用户下单.
   *
   * @param ordersSubmitDto 订单提交DTO
   * @return 订单提交VO
   */
  @Override
  @Transactional
  public OrderSubmitVo submitOrder(OrdersSubmitDto ordersSubmitDto) {
    Long userId = BaseContext.getCurrentId();

    List<ShoppingCart> shoppingCartList = getValidatedShoppingCart(userId);
    BigDecimal totalAmount = calculateTotalAmount(shoppingCartList,
        ordersSubmitDto.getPackAmount());

    // 订单属性拷贝：使用 MapStruct 将 DTO 转换为 Entity
    Orders orders = OrderConverter.INSTANCE.d2e(ordersSubmitDto);

    // 安全修复：使用服务端计算的金额覆盖客户端传来的金额，防止金额被篡改
    orders.setAmount(totalAmount);

    // 填充订单的空属性
    orders.setUserId(userId);
    LocalDateTime now = LocalDateTime.now();
    orders.setNumber(generateOrderNumber(userId, now));
    orders.setStatus(Orders.PENDING_PAYMENT);
    orders.setPayStatus(Orders.UN_PAID);
    orders.setOrderTime(now);

    fillOrderAddressAndUser(orders, ordersSubmitDto.getAddressBookId(), userId);

    // 插入订单到数据库
    this.save(orders);

    // 将购物车条目转换为订单明细
    List<OrderDetail> orderDetailList = OrderDetailConverter.INSTANCE.cartList2DetailList(
        shoppingCartList);

    // 设置订单ID并填充字段
    Long orderId = orders.getId();
    orderDetailList.forEach(orderDetail -> orderDetail.setOrderId(orderId));

    // 批量保存订单明细到数据库
    orderDetailService.saveBatch(orderDetailList);

    // 提交订单成功后清空购物车
    shoppingCartService.cleanShoppingCart();

    return buildOrderSubmitVo(orders);

  }

  private List<ShoppingCart> getValidatedShoppingCart(Long userId) {
    boolean exists = Db.lambdaQuery(ShoppingCart.class)
        .eq(ShoppingCart::getUserId, userId)
        .exists();
    if (!exists) {
      throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
    }

    List<ShoppingCart> shoppingCartList = shoppingCartService.showShoppingCart();
    if (CollectionUtils.isEmpty(shoppingCartList)) {
      throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
    }

    return shoppingCartList;
  }

  private BigDecimal calculateTotalAmount(List<ShoppingCart> shoppingCartList, Integer packAmount) {
    BigDecimal totalAmount = shoppingCartList.stream()
        .filter(cart -> cart.getAmount() != null && cart.getNumber() != null)
        .map(cart -> cart.getAmount().multiply(BigDecimal.valueOf(cart.getNumber())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (packAmount != null && packAmount > 0) {
      totalAmount = totalAmount.add(BigDecimal.valueOf(packAmount));
    }

    return totalAmount;
  }

  private String generateOrderNumber(Long userId, LocalDateTime now) {
    String userIdStr = userId.toString();
    String userIdSuffix = userIdStr.length() >= 4 ? userIdStr.substring(userIdStr.length() - 4) :
        String.format("%04d", userId);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ORDER_NUMBER_DATE_PATTERN);

    String orderNumber = generateOrderNumberSuffix(now, formatter, userIdSuffix);
    int retryCount = 0;
    while (retryCount < 5 && this.lambdaQuery().eq(Orders::getNumber, orderNumber).exists()) {
      orderNumber = generateOrderNumberSuffix(now, formatter, userIdSuffix);
      retryCount++;
    }
    if (retryCount >= 5) {
      long nanoTime = System.nanoTime();
      String nanoSuffix = String.valueOf(nanoTime)
          .substring(Math.max(0, String.valueOf(nanoTime).length() - 6));
      orderNumber = now.format(formatter) + userIdSuffix + nanoSuffix;
    }

    return orderNumber;
  }

  private String generateOrderNumberSuffix(LocalDateTime now, DateTimeFormatter formatter,
      String userIdSuffix) {
    int randomNum = ThreadLocalRandom.current().nextInt(ORDER_NUMBER_RANDOM_BOUND);
    return String.format("%s%s%03d", now.format(formatter), userIdSuffix, randomNum);
  }

  private void fillOrderAddressAndUser(Orders orders, Long addressBookId, Long userId) {
    AddressBook addressBook = addressBookService.getByIdWithCheck(addressBookId);
    orders.setConsignee(addressBook.getConsignee());
    orders.setPhone(addressBook.getPhone());
    orders.setAddress(buildAddress(addressBook));

    User user = userService.getById(userId);
    if (user != null && user.getName() != null) {
      orders.setUserName(user.getName());
    }
  }

  private String buildAddress(AddressBook addressBook) {
    return (addressBook.getProvinceName() != null ? addressBook.getProvinceName() : "")
        + (addressBook.getCityName() != null ? addressBook.getCityName() : "")
        + (addressBook.getDistrictName() != null ? addressBook.getDistrictName() : "")
        + (addressBook.getDetail() != null ? addressBook.getDetail() : "");
  }

  private OrderSubmitVo buildOrderSubmitVo(Orders orders) {
    return OrderSubmitVo.builder()
        .id(orders.getId())
        .orderNumber(orders.getNumber())
        .orderAmount(orders.getAmount())
        .orderTime(orders.getOrderTime())
        .build();
  }

  private void applyRefund(Orders target, Orders source) {
    target.setPayStatus(Orders.REFUND);
    log.info(REFUND_LOG_MESSAGE, source.getNumber());
  }

  private void fillCancelInfo(Orders orders, String reason, boolean rejection) {
    orders.setStatus(Orders.CANCELLED);
    if (rejection) {
      orders.setRejectionReason(reason);
    } else {
      orders.setCancelReason(reason);
    }
    orders.setCancelTime(LocalDateTime.now());
  }

  /**
   * 订单支付（模拟支付，不调用微信支付接口）.
   *
   * @param ordersPaymentDto 订单支付DTO
   */
  @Override
  @Transactional
  public void payment(OrdersPaymentDto ordersPaymentDto) {
    Long userId = BaseContext.getCurrentId();

    // 根据订单号和用户ID查询订单（防止订单号重复导致查询错单）
    Orders orders = Db.lambdaQuery(Orders.class)
        .eq(Orders::getNumber, ordersPaymentDto.getOrderNumber())
        .eq(Orders::getUserId, userId)
        .one();

    // 验证订单是否存在
    if (orders == null) {
      throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    // 验证订单状态是否为待付款
    if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
      throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    // 验证支付状态是否为未支付
    if (!Orders.UN_PAID.equals(orders.getPayStatus())) {
      throw new OrderBusinessException("订单已支付，请勿重复支付");
    }

    // 更新订单信息
    LocalDateTime now = LocalDateTime.now();
    orders.setPayStatus(Orders.PAID); // 支付状态：已支付
    orders.setStatus(Orders.TO_BE_CONFIRMED); // 订单状态：待接单
    orders.setCheckoutTime(now); // 结账时间

    // 如果提供了支付方式，更新支付方式
    if (ordersPaymentDto.getPayMethod() != null) {
      orders.setPayMethod(ordersPaymentDto.getPayMethod());
    }

    // 更新订单到数据库
    this.updateById(orders);
    Map<String, Object> map = new HashMap<>();
    map.put("type", 1);
    map.put("orderId", orders.getId());
    map.put("content", "订单号 :" + ordersPaymentDto.getOrderNumber());
    String jsonString = JSON.toJSONString(map);

    webSocketServer.sendToAllClient(jsonString);
  }

  /**
   * 处理超时订单：将超过15分钟未支付的订单自动取消.
   */
  @Override
  @Transactional
  public void processTimeoutOrder() {
    LocalDateTime time = LocalDateTime.now().minusMinutes(15);

    // 查询超时订单：状态为待付款、支付状态为未支付、下单时间超过15分钟
    List<Orders> timeoutOrders = lambdaQuery()
        .eq(Orders::getStatus, Orders.PENDING_PAYMENT)
        .eq(Orders::getPayStatus, Orders.UN_PAID)
        .lt(Orders::getOrderTime, time)
        .list();

    if (timeoutOrders != null && !timeoutOrders.isEmpty()) {
      LocalDateTime now = LocalDateTime.now();
      // 批量更新超时订单
      timeoutOrders.forEach(order -> {
        order.setStatus(Orders.CANCELLED); // 订单状态：已取消
        order.setCancelReason("订单超时，自动取消"); // 取消原因
        order.setCancelTime(now); // 取消时间
      });

      // 批量更新到数据库
      orderServiceProvider.getObject().updateBatchById(timeoutOrders);
    }
  }

  /**
   * 处理前一天未完成的订单：将前一天的所有未完成订单标记为已完成.
   */
  @Override
  @Transactional
  public void processDeliveryOrder() {
    LocalDateTime now = LocalDateTime.now();
    // 前一天开始时间：昨天00:00:00
    LocalDateTime yesterdayStart = now.toLocalDate().minusDays(1).atStartOfDay();
    // 前一天结束时间：今天00:00:00
    LocalDateTime yesterdayEnd = now.toLocalDate().atStartOfDay();

    // 查询前一天的所有派送中订单
    List<Orders> incompleteOrders = lambdaQuery()
        .ge(Orders::getOrderTime, yesterdayStart)
        .lt(Orders::getOrderTime, yesterdayEnd)
        .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
        .list();

    if (incompleteOrders != null && !incompleteOrders.isEmpty()) {
      // 批量更新订单为已完成
      incompleteOrders.forEach(order -> {
        order.setStatus(Orders.COMPLETED); // 订单状态：已完成
        // 如果还没有送达时间，设置送达时间
        if (order.getDeliveryTime() == null) {
          order.setDeliveryTime(now);
        }
      });

      // 批量更新到数据库
      orderServiceProvider.getObject().updateBatchById(incompleteOrders);
    }
  }

  /**
   * 用户端订单分页查询.
   *
   * @param pageNum  页码
   * @param pageSize 每页大小
   * @param status   订单状态
   * @return 分页结果
   */
  @Override
  public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
    // 设置分页
    Page<Orders> pageInfo = new Page<>(pageNum, pageSize);

    Long userId = BaseContext.getCurrentId();

    // 分页条件查询
    lambdaQuery()
        .eq(Orders::getUserId, userId)
        .eq(status != null, Orders::getStatus, status)
        .orderByDesc(Orders::getOrderTime)
        .page(pageInfo);

    List<OrderVo> list = new ArrayList<>();

    // 查询出订单明细，并封装入OrderVO进行响应
    if (pageInfo.getTotal() > 0) {
      for (Orders orders : pageInfo.getRecords()) {
        Long orderId = orders.getId(); // 订单id

        // 查询订单明细
        List<OrderDetail> orderDetails = orderDetailService.lambdaQuery()
            .eq(OrderDetail::getOrderId, orderId)
            .list();

        OrderVo orderVo = new OrderVo();
        BeanUtils.copyProperties(orders, orderVo);
        orderVo.setOrderDetailList(orderDetails);

        list.add(orderVo);
      }
    }
    return new PageResult(pageInfo.getTotal(), list);
  }

  /**
   * 查询订单详情.
   *
   * @param id 订单ID
   * @return 订单详情VO
   */
  @Override
  public OrderVo details(Long id) {
    // 根据id查询订单
    Orders orders = this.getById(id);
    if (orders == null) {
      throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    // 查询该订单对应的菜品/套餐明细
    List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery()
        .eq(OrderDetail::getOrderId, orders.getId())
        .list();

    // 将该订单及其详情封装到OrderVO并返回
    OrderVo orderVo = new OrderVo();
    BeanUtils.copyProperties(orders, orderVo);
    orderVo.setOrderDetailList(orderDetailList);

    return orderVo;
  }

  private Orders getOrderByNumberForUser(String orderNumber) {
    if (!StringUtils.hasText(orderNumber)) {
      throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    Long userId = BaseContext.getCurrentId();

    // 同时使用订单号和用户ID查询，防止订单号重复导致查询错单
    Orders orders = lambdaQuery()
        .eq(Orders::getNumber, orderNumber)
        .eq(Orders::getUserId, userId)
        .one();

    if (orders == null) {
      throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    return orders;
  }

  @Override
  public OrderVo detailsByNumber(String orderNumber) {
    Orders orders = getOrderByNumberForUser(orderNumber);

    List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery()
        .eq(OrderDetail::getOrderId, orders.getId())
        .list();

    OrderVo orderVo = new OrderVo();
    BeanUtils.copyProperties(orders, orderVo);
    orderVo.setOrderDetailList(orderDetailList);

    return orderVo;
  }

  /**
   * 用户取消订单.
   *
   * @param id 订单ID
   */
  @Override
  @Transactional
  public void userCancelById(Long id) {
    // 根据id查询订单
    Orders ordersDb = this.getById(id);

    // 校验订单是否存在
    if (ordersDb == null) {
      throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    // 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
    if (ordersDb.getStatus() > 2) {
      throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    Orders orders = new Orders();
    orders.setId(ordersDb.getId());

    // 订单处于待接单状态下取消，需要进行退款（模拟退款，直接标记为已退款）
    if (ordersDb.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
      applyRefund(orders, ordersDb);
    }

    // 更新订单状态、取消原因、取消时间
    fillCancelInfo(orders, "用户取消", false);
    updateById(orders);
  }

  @Override
  @Transactional
  public void userCancelByNumber(String orderNumber) {
    Orders orders = getOrderByNumberForUser(orderNumber);
    orderServiceProvider.getObject().userCancelById(orders.getId());
  }

  /**
   * 再来一单.
   *
   * @param id 订单ID
   */
  @Override
  @Transactional
  public void repetition(Long id) {
    // 查询当前用户id
    Long userId = BaseContext.getCurrentId();

    // 根据订单id查询当前订单详情
    List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery()
        .eq(OrderDetail::getOrderId, id)
        .list();

    // 将订单详情对象转换为购物车对象
    List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
      ShoppingCart shoppingCart = new ShoppingCart();

      // 将原订单详情里面的菜品信息重新复制到购物车对象中
      BeanUtils.copyProperties(x, shoppingCart, "id");
      shoppingCart.setUserId(userId);
      shoppingCart.setCreateTime(LocalDateTime.now());

      return shoppingCart;
    }).toList();

    // 将购物车对象批量添加到数据库
    shoppingCartService.saveBatch(shoppingCartList);
  }

  /**
   * 订单搜索.
   *
   * @param ordersPageQueryDto 订单分页查询DTO
   * @return 分页结果
   */
  @Override
  public PageResult conditionSearch(OrdersPageQueryDto ordersPageQueryDto) {
    Page<Orders> pageInfo = new Page<>(ordersPageQueryDto.getPage(),
        ordersPageQueryDto.getPageSize());

    // 使用 lambdaQuery 链式调用构建查询条件并执行分页查询
    lambdaQuery()
        .like(StringUtils.hasText(ordersPageQueryDto.getNumber()),
            Orders::getNumber, ordersPageQueryDto.getNumber())
        .like(StringUtils.hasText(ordersPageQueryDto.getPhone()),
            Orders::getPhone, ordersPageQueryDto.getPhone())
        .eq(ordersPageQueryDto.getUserId() != null,
            Orders::getUserId, ordersPageQueryDto.getUserId())
        .eq(ordersPageQueryDto.getStatus() != null,
            Orders::getStatus, ordersPageQueryDto.getStatus())
        .ge(ordersPageQueryDto.getBeginTime() != null,
            Orders::getOrderTime, ordersPageQueryDto.getBeginTime())
        .le(ordersPageQueryDto.getEndTime() != null,
            Orders::getOrderTime, ordersPageQueryDto.getEndTime())
        .orderByDesc(Orders::getOrderTime)
        .page(pageInfo);

    // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
    List<OrderVo> orderVoList = getOrderVoList(pageInfo);

    return new PageResult(pageInfo.getTotal(), orderVoList);
  }

  private List<OrderVo> getOrderVoList(Page<Orders> page) {
    // 需要返回订单菜品信息，自定义OrderVO响应结果
    List<OrderVo> orderVoList = new ArrayList<>();

    List<Orders> ordersList = page.getRecords();
    if (!CollectionUtils.isEmpty(ordersList)) {
      for (Orders orders : ordersList) {
        // 将共同字段复制到OrderVO
        OrderVo orderVo = new OrderVo();
        BeanUtils.copyProperties(orders, orderVo);
        String orderDishes = getOrderDishesStr(orders);

        // 将订单菜品信息封装到orderVo中，并添加到orderVoList
        orderVo.setOrderDishes(orderDishes);
        orderVoList.add(orderVo);
      }
    }
    return orderVoList;
  }

  /**
   * 根据订单id获取菜品信息字符串.
   *
   * @param orders 订单信息
   * @return 菜品信息字符串
   */
  private String getOrderDishesStr(Orders orders) {
    // 查询订单菜品详情信息（订单中的菜品和数量）
    List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery()
        .eq(OrderDetail::getOrderId, orders.getId())
        .list();

    // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
    List<String> orderDishList = orderDetailList.stream()
        .map(x -> x.getName() + "*" + x.getNumber() + ";")
        .toList();

    // 将该订单对应的所有菜品信息拼接在一起
    return String.join("", orderDishList);
  }

  /**
   * 各个状态的订单数量统计.
   *
   * @return 订单状态统计结果
   */
  @Override
  public OrderStatisticsVo statistics() {
    // 根据状态，分别查询出待接单、待派送、派送中的订单数量
    Integer toBeConfirmed = lambdaQuery()
        .eq(Orders::getStatus, Orders.TO_BE_CONFIRMED)
        .count().intValue();
    Integer confirmed = lambdaQuery()
        .eq(Orders::getStatus, Orders.CONFIRMED)
        .count().intValue();
    Integer deliveryInProgress = lambdaQuery()
        .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
        .count().intValue();

    // 将查询出的数据封装到orderStatisticsVO中响应
    OrderStatisticsVo orderStatisticsVo = new OrderStatisticsVo();
    orderStatisticsVo.setToBeConfirmed(toBeConfirmed);
    orderStatisticsVo.setConfirmed(confirmed);
    orderStatisticsVo.setDeliveryInProgress(deliveryInProgress);
    return orderStatisticsVo;
  }

  /**
   * 接单.
   *
   * @param ordersConfirmDto 订单确认DTO
   */
  @Override
  @Transactional
  public void confirm(OrdersConfirmDto ordersConfirmDto) {
    Orders orders = Orders.builder()
        .id(ordersConfirmDto.getId())
        .status(Orders.CONFIRMED)
        .build();

    this.updateById(orders);
  }

  /**
   * 拒单.
   *
   * @param ordersRejectionDto 订单拒单DTO
   */
  @Override
  @Transactional
  public void rejection(OrdersRejectionDto ordersRejectionDto) {
    // 根据id查询订单
    Orders ordersDb = this.getById(ordersRejectionDto.getId());

    // 订单只有存在且状态为2（待接单）才可以拒单
    if (ordersDb == null || !ordersDb.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
      throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    // 支付状态
    Integer payStatus = ordersDb.getPayStatus();
    Orders orders = new Orders();
    orders.setId(ordersDb.getId());

    if (Orders.PAID.equals(payStatus)) {
      // 用户已支付，需要退款（模拟退款，直接标记为已退款）
      applyRefund(orders, ordersDb);
    }

    // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
    fillCancelInfo(orders, ordersRejectionDto.getRejectionReason(), true);

    updateById(orders);
  }

  /**
   * 取消订单.
   *
   * @param ordersCancelDto 订单取消DTO
   */
  @Override
  @Transactional
  public void cancel(OrdersCancelDto ordersCancelDto) {
    // 根据id查询订单
    Orders ordersDb = this.getById(ordersCancelDto.getId());

    // 校验订单是否存在
    if (ordersDb == null) {
      throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    // 支付状态
    Integer payStatus = ordersDb.getPayStatus();
    Orders orders = new Orders();
    orders.setId(ordersCancelDto.getId());

    if (Orders.PAID.equals(payStatus)) {
      // 用户已支付，需要退款（模拟退款，直接标记为已退款）
      applyRefund(orders, ordersDb);
    }

    // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
    fillCancelInfo(orders, ordersCancelDto.getCancelReason(), false);
    updateById(orders);
  }

  /**
   * 派送订单.
   *
   * @param id 订单ID
   */
  @Override
  @Transactional
  public void delivery(Long id) {
    // 根据id查询订单
    Orders ordersDb = this.getById(id);

    // 校验订单是否存在，并且状态为3
    if (ordersDb == null || !ordersDb.getStatus().equals(Orders.CONFIRMED)) {
      throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    Orders orders = new Orders();
    orders.setId(ordersDb.getId());
    // 更新订单状态,状态转为派送中
    orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

    updateById(orders);
  }

  /**
   * 完成订单.
   *
   * @param id 订单ID
   */
  @Override
  @Transactional
  public void complete(Long id) {
    // 根据id查询订单
    Orders ordersDb = this.getById(id);

    // 校验订单是否存在，并且状态为4
    if (ordersDb == null || !ordersDb.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
      throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    Orders orders = new Orders();
    orders.setId(ordersDb.getId());
    // 更新订单状态,状态转为完成
    orders.setStatus(Orders.COMPLETED);
    orders.setDeliveryTime(LocalDateTime.now());

    updateById(orders);
  }

  /**
   * 催单.
   *
   * @param id 订单ID
   */
  @Override
  public void reminder(Long id) {
    // 根据id查询订单
    Orders ordersDb = this.getById(id);

    // 校验订单是否存在
    if (ordersDb == null) {
      throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    // 构造催单消息，用ws推送消息给商家
    Map<String, Object> map = new HashMap<>();
    map.put("type", 2); // 1表示来单提醒 2表示客户催单
    map.put("orderId", id);
    map.put("content", "订单号:" + ordersDb.getNumber());
    String jsonString = JSON.toJSONString(map);

    webSocketServer.sendToAllClient(jsonString);
  }

  @Override
  public void repetitionByNumber(String orderNumber) {
    Orders orders = getOrderByNumberForUser(orderNumber);
    orderServiceProvider.getObject().repetition(orders.getId());
  }

  @Override
  public void reminderByNumber(String orderNumber) {
    Orders orders = getOrderByNumberForUser(orderNumber);
    reminder(orders.getId());
  }
}
