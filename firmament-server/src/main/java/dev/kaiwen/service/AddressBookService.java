package dev.kaiwen.service;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.kaiwen.entity.AddressBook;
import java.util.List;

/**
 * 地址簿服务接口.
 */
public interface AddressBookService extends IService<AddressBook> {

  /**
   * 查询当前用户的所有地址信息.
   *
   * @return 地址列表
   */
  @Override
  List<AddressBook> list();

  /**
   * 新增地址.
   *
   * @param addressBook 地址信息
   */
  void saveAddress(AddressBook addressBook);

  /**
   * 根据id修改地址.
   *
   * @param addressBook 地址信息
   */
  void updateAddress(AddressBook addressBook);

  /**
   * 设置默认地址.
   *
   * @param addressBook 地址信息
   */
  void setDefault(AddressBook addressBook);

  /**
   * 查询默认地址.
   *
   * @return 默认地址
   */
  AddressBook getDefault();

  /**
   * 根据id查询地址（带归属校验）.
   *
   * @param id 地址ID
   * @return 地址信息
   */
  AddressBook getByIdWithCheck(Long id);

  /**
   * 根据id删除地址（带归属校验）.
   *
   * @param id 地址ID
   */
  void removeByIdWithCheck(Long id);
}

