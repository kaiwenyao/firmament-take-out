package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import dev.kaiwen.constant.MessageConstant;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.converter.OrderConverter;
import dev.kaiwen.converter.OrderDetailConverter;
import dev.kaiwen.dto.OrdersSubmitDTO;
import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.entity.ShoppingCart;
import dev.kaiwen.exception.AddressBookBusinessException;
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

        // 14. 构建并返回 OrderSubmitVO
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

    }
}
