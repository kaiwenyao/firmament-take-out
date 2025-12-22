package dev.kaiwen.converter;

import dev.kaiwen.dto.EmployeeDTO;
import dev.kaiwen.entity.Employee;
import org.mapstruct.Mapper;


// ▼ componentModel = "spring" 是灵魂！
// 加上它，MapStruct 会自动加上 @Component 注解，你就可以在 Service 里 @Autowired 了
@Mapper(componentModel = "spring") 
public interface EmployeeConverter {

    // 不需要写实现类，MapStruct 编译时会自动生成！

    // 1. DTO -> Entity (新增员工时用)
    Employee d2e(EmployeeDTO employeeDTO);

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