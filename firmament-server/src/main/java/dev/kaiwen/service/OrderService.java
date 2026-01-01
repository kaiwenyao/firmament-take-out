package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.dto.*;
import dev.kaiwen.entity.Orders;
import dev.kaiwen.result.PageResult;
import dev.kaiwen.vo.OrderStatisticsVO;
import dev.kaiwen.vo.OrderSubmitVO;
import dev.kaiwen.vo.OrderVO;

public interface OrderService extends IService<Orders> {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
    
    void payment(OrdersPaymentDTO ordersPaymentDTO);
    
    /**
     * 处理超时订单
     */
    void processTimeoutOrder();
    
    /**
     * 处理前一天未完成的订单：将前一天的所有未完成订单标记为已完成
     */
    void processDeliveryOrder();
    
    /**
     * 用户端订单分页查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);
    
    /**
     * 查询订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);
    
    /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id) throws Exception;

    void userCancelByNumber(String orderNumber) throws Exception;
    
    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);
    
    /**
     * 条件搜索订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);
    
    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();
    
    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);
    
    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;
    
    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;
    
    /**
     * 派送订单
     * @param id
     */
    void delivery(Long id);
    
    /**
     * 完成订单
     * @param id
     */
    void complete(Long id);

    void reminder(Long id);

    OrderVO detailsByNumber(String orderNumber);

    void repetitionByNumber(String orderNumber);

    void reminderByNumber(String orderNumber);
}
