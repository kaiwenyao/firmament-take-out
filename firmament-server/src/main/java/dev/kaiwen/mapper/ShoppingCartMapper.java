package dev.kaiwen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.kaiwen.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车 Mapper 接口.
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

}
