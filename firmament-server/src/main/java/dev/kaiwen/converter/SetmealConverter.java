package dev.kaiwen.converter;

import dev.kaiwen.dto.SetmealDto;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.vo.SetmealVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Setmeal 转换器
 * 使用 MapStruct 自动生成实现类
 */
@Mapper
public interface SetmealConverter {
    
    SetmealConverter INSTANCE = Mappers.getMapper(SetmealConverter.class);
    
    /**
     * DTO -> Entity (用于新增和修改套餐)
     * @param setmealDTO 套餐DTO
     * @return 套餐实体
     */
    Setmeal d2e(SetmealDto setmealDTO);

    /**
     * Entity -> VO (用于查询返回)
     * @param setmeal 套餐实体
     * @return 套餐VO
     */
    SetmealVo e2v(Setmeal setmeal);

    /**
     * Entity List -> VO List (用于批量查询返回)
     * @param list 套餐实体列表
     * @return 套餐VO列表
     */
    List<SetmealVo> eL2v(List<Setmeal> list);
}

