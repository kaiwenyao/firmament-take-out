package dev.kaiwen.converter;

import dev.kaiwen.dto.EmployeeDto;
import dev.kaiwen.entity.Employee;
import dev.kaiwen.vo.EmployeeVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Employee 转换器
 * 使用 MapStruct 自动生成实现类
 * 不需要写实现类，MapStruct 编译时会自动生成！
 */
@Mapper
public interface EmployeeConverter {

    EmployeeConverter INSTANCE = Mappers.getMapper(EmployeeConverter.class);

    // 1. DTO -> Entity (新增员工时用)
    Employee d2e(EmployeeDto employeeDTO);

    EmployeeVO e2v(Employee  employee);
    List<EmployeeVO> toVOList(List<Employee> list);

    // 新增：把 DTO 转成 Entity (用于修改操作)
    // 2. Entity -> VO (返回给前端时用)
//    EmployeeVO toVO(Employee employee);
    
    // 3. 集合转换 List<Entity> -> List<VO>
//    List<EmployeeVO> toVOList(List<Employee> list);

    // 4. 高级用法：如果字段名不一样
    // 假设 DTO 里叫 username，但 Entity 里叫 name
    // @Mapping(source = "username", target = "name")
    // Employee toEntityCustom(EmployeeDTO dto);
}