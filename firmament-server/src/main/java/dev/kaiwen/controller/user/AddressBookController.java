package dev.kaiwen.controller.user;

import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.AddressBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端地址簿接口
 */
@RestController
@Slf4j
@RequestMapping("/user/addressBook")
@Tag(name = "C端地址簿接口")
@RequiredArgsConstructor
public class AddressBookController {

    private final AddressBookService addressBookService;

    /**
     * 查询当前登录用户的所有地址信息
     * @return 地址列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询当前登录用户的所有地址信息")
    public Result<List<AddressBook>> list() {
        log.info("查询当前用户的所有地址信息");
        List<AddressBook> list = addressBookService.list();
        return Result.success(list);
    }

    /**
     * 新增地址
     * @param addressBook 地址信息
     * @return
     */
    @PostMapping
    @Operation(summary = "新增地址")
    public Result save(@RequestBody AddressBook addressBook) {
        log.info("新增地址：{}", addressBook);
        addressBookService.saveAddress(addressBook);
        return Result.success();
    }

    /**
     * 根据id查询地址
     * @param id 地址ID
     * @return 地址信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询地址")
    public Result<AddressBook> getById(@PathVariable Long id) {
        log.info("根据id查询地址：{}", id);
        AddressBook addressBook = addressBookService.getByIdWithCheck(id);
        return Result.success(addressBook);
    }

    /**
     * 根据id修改地址
     * @param addressBook 地址信息
     * @return
     */
    @PutMapping
    @Operation(summary = "根据id修改地址")
    public Result update(@RequestBody AddressBook addressBook) {
        log.info("修改地址：{}", addressBook);
        addressBookService.updateAddress(addressBook);
        return Result.success();
    }

    /**
     * 设置默认地址
     * @param addressBook 地址信息
     * @return
     */
    @PutMapping("/default")
    @Operation(summary = "设置默认地址")
    public Result setDefault(@RequestBody AddressBook addressBook) {
        log.info("设置默认地址：{}", addressBook.getId());
        addressBookService.setDefault(addressBook);
        return Result.success();
    }

    /**
     * 根据id删除地址
     * @param id 地址ID
     * @return
     */
    @DeleteMapping
    @Operation(summary = "根据id删除地址")
    public Result deleteById(@RequestParam Long id) {
        log.info("删除地址：{}", id);
        addressBookService.removeByIdWithCheck(id);
        return Result.success();
    }

    /**
     * 查询默认地址
     * @return 默认地址
     */
    @GetMapping("/default")
    @Operation(summary = "查询默认地址")
    public Result<AddressBook> getDefault() {
        log.info("查询默认地址");
        AddressBook addressBook = addressBookService.getDefault();
        if (addressBook != null) {
            return Result.success(addressBook);
        }
        return Result.error("没有查询到默认地址");
    }
}
