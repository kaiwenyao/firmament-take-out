package dev.kaiwen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.kaiwen.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    // 使用了mybatis plus 下面这个查询直接移动到service层
/*    *//**
     * 根据用户名查询员工
     * @param username
     * @return
     *//*
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);*/

}
