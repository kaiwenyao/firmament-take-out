package dev.kaiwen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.kaiwen.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper 接口.
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

}
