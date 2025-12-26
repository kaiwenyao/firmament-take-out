package dev.kaiwen.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.OrderConverter;
import dev.kaiwen.converter.OrderDetailConverter;
import dev.kaiwen.dto.*;
import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.exception.AddressBookBusinessException;
import dev.kaiwen.exception.OrderBusinessException;
import dev.kaiwen.exception.ShoppingCartBusinessException;
import dev.kaiwen.mapper.OrderMapper;
import dev.kaiwen.entity.User;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.service.AddressBookService;
import dev.kaiwen.service.OrderDetailService;
import dev.kaiwen.service.OrderService;
import dev.kaiwen.service.ShoppingCartService;
import dev.kaiwen.service.UserService;
import dev.kaiwen.vo.OrderStatisticsVO;
import dev.kaiwen.vo.OrderSubmitVO;
import dev.kaiwen.vo.OrderVO;
import dev.kaiwen.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    private final OrderDetailService orderDetailService;
    private final AddressBookService addressBookService;
    private final ShoppingCartService shoppingCartService;
    private final UserService userService;
    private final OrderConverter orderConverter;
    private final OrderDetailConverter orderDetailConverter;
    private final WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        boolean exists = Db.lambdaQuery(AddressBook.class)
                .eq(AddressBook::getId, ordersSubmitDTO.getAddressBookId())
                .exists();
        if (!exists) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        exists = Db.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, userId)
                .exists();
        if (!exists) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        
        // 订单属性拷贝：使用 MapStruct 将 DTO 转换为 Entity
        Orders orders = orderConverter.d2e(ordersSubmitDTO);
        
        // 查询地址信息（前面已验证地址存在，直接获取）
        AddressBook addressBook = addressBookService.getById(ordersSubmitDTO.getAddressBookId());
        
        // 填充订单的空属性
        // 1. 设置用户ID
        orders.setUserId(userId);
        
        // 2. 生成订单号：时间戳格式 yyyyMMddHHmmss + 用户ID（如果用户ID长度>=4则取后4位，否则取全部）
        LocalDateTime now = LocalDateTime.now();
        String userIdStr = userId.toString();
        String suffix = userIdStr.length() >= 4 ? userIdStr.substring(userIdStr.length() - 4) : userIdStr;
        String orderNumber = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + suffix;
        orders.setNumber(orderNumber);
        
        // 3. 设置订单状态：待付款
        orders.setStatus(Orders.PENDING_PAYMENT);
        
        // 4. 设置支付状态：未支付
        orders.setPayStatus(Orders.UN_PAID);
        
        // 5. 设置下单时间
        orders.setOrderTime(now);
        
        // 6. 设置收货人信息
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        
        // 7. 拼接地址：省+市+区+详细地址
        String address = (addressBook.getProvinceName() != null ? addressBook.getProvinceName() : "")
                + (addressBook.getCityName() != null ? addressBook.getCityName() : "")
                + (addressBook.getDistrictName() != null ? addressBook.getDistrictName() : "")
                + (addressBook.getDetail() != null ? addressBook.getDetail() : "");
        orders.setAddress(address);
        
        // 8. 查询用户信息并设置用户名
        User user = userService.getById(userId);
        if (user != null && user.getName() != null) {
            orders.setUserName(user.getName());
        }
        
        // 9. 插入订单到数据库
        this.save(orders);
        
        // 10. 获取当前用户的购物车列表
        List<ShoppingCart> shoppingCartList = shoppingCartService.showShoppingCart();
        
        // 11. 将购物车条目转换为订单明细
        List<OrderDetail> orderDetailList = orderDetailConverter.cartList2DetailList(shoppingCartList);
        
        // 12. 设置订单ID并填充字段
        Long orderId = orders.getId();
        orderDetailList.forEach(orderDetail -> {
            orderDetail.setOrderId(orderId);
        });
        
        // 13. 批量保存订单明细到数据库
        orderDetailService.saveBatch(orderDetailList);
        
        // 14. 提交订单成功后清空购物车
        shoppingCartService.cleanShoppingCart();

        // 15. 构建并返回 OrderSubmitVO
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

    }

    /**
     * 订单支付（模拟支付，不调用微信支付接口）
     * @param ordersPaymentDTO
     */
    @Override
    @Transactional
    public void payment(OrdersPaymentDTO ordersPaymentDTO) {
        Long userId = BaseContext.getCurrentId();
        
        // 根据订单号查询订单
        Orders orders = Db.lambdaQuery(Orders.class)
                .eq(Orders::getNumber, ordersPaymentDTO.getOrderNumber())
                .one();
        
        // 验证订单是否存在
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        
        // 验证订单是否属于当前用户
        if (!orders.getUserId().equals(userId)) {
            throw new OrderBusinessException("订单不属于当前用户");
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
        if (ordersPaymentDTO.getPayMethod() != null) {
            orders.setPayMethod(ordersPaymentDTO.getPayMethod());
        }
        
        // 更新订单到数据库
        this.updateById(orders);
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);
        map.put("orderId", orders.getId());
        map.put("content", "订单号 :" + ordersPaymentDTO.getOrderNumber());
        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 处理超时订单：将超过15分钟未支付的订单自动取消
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
            this.updateBatchById(timeoutOrders);
        }
    }

    /**
     * 处理前一天未完成的订单：将前一天的所有未完成订单标记为已完成
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
            this.updateBatchById(incompleteOrders);
        }
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
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

        List<OrderVO> list = new ArrayList<>();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (pageInfo != null && pageInfo.getTotal() > 0) {
            for (Orders orders : pageInfo.getRecords()) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailService.lambdaQuery()
                        .eq(OrderDetail::getOrderId, orderId)
                        .list();

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(pageInfo.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
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
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = this.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款（模拟退款，直接标记为已退款）
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            // 支付状态修改为 退款（模拟退款，不调用真实支付接口）
            orders.setPayStatus(Orders.REFUND);
            log.info("订单 {} 已退款（模拟）", ordersDB.getNumber());
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        this.updateById(orders);
    }

    /**
     * 再来一单
     *
     * @param id
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
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartService.saveBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        Page<Orders> pageInfo = new Page<>(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 使用 lambdaQuery 链式调用构建查询条件并执行分页查询
        lambdaQuery()
                .like(StringUtils.hasText(ordersPageQueryDTO.getNumber()),
                        Orders::getNumber, ordersPageQueryDTO.getNumber())
                .like(StringUtils.hasText(ordersPageQueryDTO.getPhone()),
                        Orders::getPhone, ordersPageQueryDTO.getPhone())
                .eq(ordersPageQueryDTO.getUserId() != null,
                        Orders::getUserId, ordersPageQueryDTO.getUserId())
                .eq(ordersPageQueryDTO.getStatus() != null,
                        Orders::getStatus, ordersPageQueryDTO.getStatus())
                .ge(ordersPageQueryDTO.getBeginTime() != null,
                        Orders::getOrderTime, ordersPageQueryDTO.getBeginTime())
                .le(ordersPageQueryDTO.getEndTime() != null,
                        Orders::getOrderTime, ordersPageQueryDTO.getEndTime())
                .orderByDesc(Orders::getOrderTime)
                .page(pageInfo);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(pageInfo);

        return new PageResult(pageInfo.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getRecords();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailService.lambdaQuery()
                .eq(OrderDetail::getOrderId, orders.getId())
                .list();

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
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
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Override
    @Transactional
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        this.updateById(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = this.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款（模拟退款，直接标记为已退款）
            orders.setPayStatus(Orders.REFUND);
            log.info("订单 {} 已退款（模拟）", ordersDB.getNumber());
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        this.updateById(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    @Transactional
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = this.getById(ordersCancelDTO.getId());
        
        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款（模拟退款，直接标记为已退款）
            orders.setPayStatus(Orders.REFUND);
            log.info("订单 {} 已退款（模拟）", ordersDB.getNumber());
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        this.updateById(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = this.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        this.updateById(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    @Transactional
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = this.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        this.updateById(orders);
    }

    /**
     * 催单
     *
     * @param id
     */
    @Override
    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = this.getById(id);
        
        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 构造催单消息，用ws推送消息给商家
        Map<String, Object> map = new HashMap<>();
        map.put("type", 2); // 1表示来单提醒 2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号:" + ordersDB.getNumber());
        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }
}
