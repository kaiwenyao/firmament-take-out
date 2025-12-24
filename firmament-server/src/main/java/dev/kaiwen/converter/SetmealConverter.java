package dev.kaiwen.converter;

import dev.kaiwen.dto.SetmealDTO;
import dev.kaiwen.entity.Setmeal;
import dev.kaiwen.vo.SetmealVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Setmeal 转换器
 * 使用 MapStruct 自动生成实现类
 */
@Mapper(componentModel = "spring")
public interface SetmealConverter {
    /**
     * DTO -> Entity (用于新增和修改套餐)
     * @param setmealDTO 套餐DTO
     * @return 套餐实体
     */
    Setmeal d2e(SetmealDTO setmealDTO);

    /**
     * Entity -> VO (用于查询返回)
     * @param setmeal 套餐实体
     * @return 套餐VO
     */
    SetmealVO e2v(Setmeal setmeal);

    /**
     * Entity List -> VO List (用于批量查询返回)
     * @param list 套餐实体列表
     * @return 套餐VO列表
     */
    List<SetmealVO> eL2v(List<Setmeal> list);
}

