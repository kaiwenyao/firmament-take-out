package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.OrderConverter;
import dev.kaiwen.converter.OrderDetailConverter;
import dev.kaiwen.dto.OrdersPaymentDTO;
import dev.kaiwen.dto.OrdersSubmitDTO;
import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.exception.AddressBookBusinessException;
import dev.kaiwen.exception.OrderBusinessException;
import dev.kaiwen.exception.ShoppingCartBusinessException;
import dev.kaiwen.mapper.OrderMapper;
import dev.kaiwen.entity.User;
import dev.kaiwen.service.IAddressBookService;
import dev.kaiwen.service.IOrderDetailService;
import dev.kaiwen.service.IOrderService;
import dev.kaiwen.service.IShoppingCartService;
import dev.kaiwen.service.IUserService;
import dev.kaiwen.vo.OrderSubmitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements IOrderService {

    private final IOrderDetailService orderDetailService;
    private final IAddressBookService addressBookService;
    private final IShoppingCartService shoppingCartService;
    private final IUserService userService;
    private final OrderConverter orderConverter;
    private final OrderDetailConverter orderDetailConverter;

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
}
