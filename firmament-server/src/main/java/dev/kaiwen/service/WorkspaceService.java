package dev.kaiwen.service;

import dev.kaiwen.vo.BusinessDataVo;
import dev.kaiwen.vo.DishOverViewVo;
import dev.kaiwen.vo.OrderOverViewVo;
import dev.kaiwen.vo.SetmealOverViewVo;

import java.time.LocalDateTime;

public interface WorkspaceService {

    /**
     * 根据时间段统计营业数据
     * @param begin 开始时间
     * @param end 结束时间
     * @return 营业数据
     */
    BusinessDataVo getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据
     * @return 订单概览数据
     */
    OrderOverViewVo getOrderOverView();

    /**
     * 查询菜品总览
     * @return 菜品概览数据
     */
    DishOverViewVo getDishOverView();

    /**
     * 查询套餐总览
     * @return 套餐概览数据
     */
    SetmealOverViewVo getSetmealOverView();
}

