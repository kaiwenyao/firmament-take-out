package dev.kaiwen.controller.user;

import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.AddressBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Address book controller for client side.
 */
@RestController
@Slf4j
@RequestMapping("/user/addressBook")
@Tag(name = "C端地址簿接口")
@RequiredArgsConstructor
public class AddressBookController {

  private final AddressBookService addressBookService;

  /**
   * Get all addresses of the current logged-in user.
   *
   * @return The list of addresses.
   */
  @GetMapping("/list")
  @Operation(summary = "查询当前登录用户的所有地址信息")
  public Result<List<AddressBook>> list() {
    log.info("查询当前用户的所有地址信息");
    List<AddressBook> list = addressBookService.list();
    return Result.success(list);
  }

  /**
   * Create a new address.
   *
   * @param addressBook The address information.
   * @return The operation result, returns success message on success.
   */
  @PostMapping
  @Operation(summary = "新增地址")
  public Result<Void> save(@RequestBody AddressBook addressBook) {
    log.info("新增地址：{}", addressBook);
    addressBookService.saveAddress(addressBook);
    return Result.success();
  }

  /**
   * Get address by ID.
   *
   * @param id The address ID.
   * @return The address information.
   */
  @GetMapping("/{id}")
  @Operation(summary = "根据id查询地址")
  public Result<AddressBook> getById(@PathVariable Long id) {
    log.info("根据id查询地址：{}", id);
    AddressBook addressBook = addressBookService.getByIdWithCheck(id);
    return Result.success(addressBook);
  }

  /**
   * Update address by ID.
   *
   * @param addressBook The address information containing address ID and updated information.
   * @return The operation result, returns success message on success.
   */
  @PutMapping
  @Operation(summary = "根据id修改地址")
  public Result<Void> update(@RequestBody AddressBook addressBook) {
    log.info("修改地址：{}", addressBook);
    addressBookService.updateAddress(addressBook);
    return Result.success();
  }

  /**
   * Set default address.
   *
   * @param addressBook The address information containing address ID.
   * @return The operation result, returns success message on success.
   */
  @PutMapping("/default")
  @Operation(summary = "设置默认地址")
  public Result<Void> setDefault(@RequestBody AddressBook addressBook) {
    log.info("设置默认地址：{}", addressBook.getId());
    addressBookService.setDefault(addressBook);
    return Result.success();
  }

  /**
   * Delete address by ID.
   *
   * @param id The address ID.
   * @return The operation result, returns success message on success.
   */
  @DeleteMapping
  @Operation(summary = "根据id删除地址")
  public Result<Void> deleteById(@RequestParam Long id) {
    log.info("删除地址：{}", id);
    addressBookService.removeByIdWithCheck(id);
    return Result.success();
  }

  /**
   * Get default address.
   *
   * @return The default address.
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
