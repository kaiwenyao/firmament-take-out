package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.entity.OrderDetail;
import dev.kaiwen.mapper.OrderDetailMapper;
import dev.kaiwen.service.OrderDetailService;
import org.springframework.stereotype.Service;

/**
 * 订单明细服务实现类.
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements
    OrderDetailService {

}
