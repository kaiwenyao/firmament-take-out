package dev.kaiwen.converter;

import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.vo.EmployeeVo;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Employee 转换器. 使用 MapStruct 自动生成实现类. 不需要写实现类，MapStruct 编译时会自动生成！
 */
@Mapper
public interface EmployeeConverter {

  EmployeeConverter INSTANCE = Mappers.getMapper(EmployeeConverter.class);

  /**
   * DTO -> Entity (用于新增员工).
   *
   * @param employeeDto 员工DTO
   * @return 员工实体
   */
  Employee d2e(EmployeeDto employeeDto);

  /**
   * Entity -> VO (用于查询返回).
   *
   * @param employee 员工实体
   * @return 员工VO
   */
  EmployeeVo e2v(Employee employee);

  /**
   * Entity List -> VO List (用于批量查询返回).
   *
   * @param list 员工实体列表
   * @return 员工VO列表
   */
  List<EmployeeVo> entityListToVoList(List<Employee> list);
}