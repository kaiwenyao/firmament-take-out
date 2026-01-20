package dev.kaiwen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.kaiwen.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper 接口.
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}
