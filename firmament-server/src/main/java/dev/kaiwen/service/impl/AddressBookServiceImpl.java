package dev.kaiwen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.mapper.AddressBookMapper;
import dev.kaiwen.service.AddressBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址簿服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {

    /**
     * 查询当前用户的所有地址信息
     * @return 地址列表
     */
    @Override
    public List<AddressBook> list() {
        Long userId = BaseContext.getCurrentId();
        
        // 使用 MyBatis Plus 的 lambdaQuery 查询当前用户的所有地址
        // 按默认地址优先排序（默认地址在前）
        return lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .orderByDesc(AddressBook::getIsDefault)
                .list();
    }

    /**
     * 新增地址
     * @param addressBook 地址信息
     */
    @Override
    public void saveAddress(AddressBook addressBook) {
        // 1. 获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        
        // 2. 设置用户ID和默认状态
        addressBook.setUserId(userId);
        addressBook.setIsDefault(0);
        
        // 3. 使用 MyBatis Plus 的 save 方法保存地址
        this.save(addressBook);
    }

    /**
     * 根据id修改地址
     * @param addressBook 地址信息
     */
    @Override
    public void updateAddress(AddressBook addressBook) {
        // 使用 MyBatis Plus 的 updateById 方法更新地址
        this.updateById(addressBook);
    }

    /**
     * 设置默认地址
     * @param addressBook 地址信息
     */
    @Override
    @Transactional
    public void setDefault(AddressBook addressBook) {
        Long userId = BaseContext.getCurrentId();
        Long addressBookId = addressBook.getId();
        
        // 1. 将当前用户的所有地址修改为非默认地址
        // 语义：UPDATE address_book SET is_default = 0 WHERE user_id = ?
        lambdaUpdate()
                .eq(AddressBook::getUserId, userId)
                .set(AddressBook::getIsDefault, 0)
                .update();
        
        // 2. 将当前地址改为默认地址
        // 语义：UPDATE address_book SET is_default = 1 WHERE id = ?
        lambdaUpdate()
                .eq(AddressBook::getId, addressBookId)
                .set(AddressBook::getIsDefault, 1)
                .update();
    }

    /**
     * 查询默认地址
     * @return 默认地址
     */
    @Override
    public AddressBook getDefault() {
        Long userId = BaseContext.getCurrentId();
        
        // 使用 MyBatis Plus 的 lambdaQuery 查询默认地址
        // 语义：SELECT * FROM address_book WHERE user_id = ? AND is_default = 1 LIMIT 1
        return lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getIsDefault, 1)
                .one();
    }
}
