package dev.kaiwen.controller.admin;

import dev.kaiwen.result.Result;
import dev.kaiwen.service.WorkspaceService;
import dev.kaiwen.vo.BusinessDataVo;
import dev.kaiwen.vo.DishOverViewVo;
import dev.kaiwen.vo.OrderOverViewVo;
import dev.kaiwen.vo.SetmealOverViewVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Workspace controller.
 */
@RestController
@RequestMapping("/admin/workspace")
@Slf4j
@Tag(name = "工作台相关接口")
@RequiredArgsConstructor
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  /**
   * Get today's business data for workspace.
   *
   * @return Today's business data.
   */
  @GetMapping("/businessData")
  @Operation(summary = "工作台今日数据查询")
  public Result<BusinessDataVo> businessData() {
    // 获得当天的开始时间
    LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
    // 获得当天的结束时间
    LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

    BusinessDataVo businessDataVo = workspaceService.getBusinessData(begin, end);
    return Result.success(businessDataVo);
  }

  /**
   * Get order overview data.
   *
   * @return The order overview data.
   */
  @GetMapping("/overviewOrders")
  @Operation(summary = "查询订单管理数据")
  public Result<OrderOverViewVo> orderOverView() {
    return Result.success(workspaceService.getOrderOverView());
  }

  /**
   * Get dish overview data.
   *
   * @return The dish overview data.
   */
  @GetMapping("/overviewDishes")
  @Operation(summary = "查询菜品总览")
  public Result<DishOverViewVo> dishOverView() {
    return Result.success(workspaceService.getDishOverView());
  }

  /**
   * Get setmeal overview data.
   *
   * @return The setmeal overview data.
   */
  @GetMapping("/overviewSetmeals")
  @Operation(summary = "查询套餐总览")
  public Result<SetmealOverViewVo> setmealOverView() {
    return Result.success(workspaceService.getSetmealOverView());
  }
}

