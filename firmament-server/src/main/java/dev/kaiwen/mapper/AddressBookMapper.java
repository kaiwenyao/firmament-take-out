package dev.kaiwen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import dev.kaiwen.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地址簿 Mapper 接口.
 */
@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {

}

