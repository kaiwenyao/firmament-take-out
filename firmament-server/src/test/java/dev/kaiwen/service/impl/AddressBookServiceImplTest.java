package dev.kaiwen.service.impl;

import static dev.kaiwen.constant.MessageConstant.ADDRESS_BOOK_ACCESS_DENIED;
import static dev.kaiwen.constant.MessageConstant.ADDRESS_BOOK_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import dev.kaiwen.context.BaseContext;
import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.exception.AddressBookBusinessException;
import dev.kaiwen.mapper.AddressBookMapper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AddressBookServiceImplTest {

  @InjectMocks
  private AddressBookServiceImpl addressBookService;

  @Mock
  private AddressBookMapper mapper;

  @Captor
  private ArgumentCaptor<AddressBook> addressCaptor;

  @Captor
  private ArgumentCaptor<LambdaQueryWrapper<AddressBook>> wrapperCaptor;

  @BeforeEach
  void setUp() {
    MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
    TableInfoHelper.initTableInfo(assistant, AddressBook.class);
    ReflectionTestUtils.setField(addressBookService, "baseMapper", mapper);
  }

  @Test
  void listSuccess() {
    AddressBook address = new AddressBook();
    address.setId(1L);
    address.setUserId(10L);
    address.setIsDefault(1);
    List<AddressBook> addressList = List.of(address);

    when(mapper.selectList(any())).thenReturn(addressList);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(10L);

      List<AddressBook> result = addressBookService.list();

      assertNotNull(result);
      assertEquals(1, result.size());
      verify(mapper).selectList(wrapperCaptor.capture());
    }
  }

  @Test
  void saveAddressSetsUserAndDefault() {
    AddressBook addressBook = new AddressBook();

    when(mapper.insert(any(AddressBook.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(20L);

      addressBookService.saveAddress(addressBook);

      verify(mapper).insert(addressCaptor.capture());
      AddressBook saved = addressCaptor.getValue();
      assertEquals(20L, saved.getUserId());
      assertEquals(0, saved.getIsDefault());
    }
  }

  @Test
  void updateAddressSuccess() {
    AddressBook update = new AddressBook();
    update.setId(100L);
    update.setDetail("detail");

    AddressBook existing = new AddressBook();
    existing.setId(100L);
    existing.setUserId(30L);

    when(mapper.selectById(100L)).thenReturn(existing);
    when(mapper.updateById(any(AddressBook.class))).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(30L);

      addressBookService.updateAddress(update);

      verify(mapper).updateById(addressCaptor.capture());
      AddressBook saved = addressCaptor.getValue();
      assertEquals(30L, saved.getUserId());
    }
  }

  @Test
  void updateAddressWithNullThrows() {
    AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
        () -> addressBookService.updateAddress(null));

    assertEquals(ADDRESS_BOOK_NOT_FOUND, exception.getMessage());
  }

  @Test
  void updateAddressWithNullIdThrows() {
    AddressBook update = new AddressBook();

    AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
        () -> addressBookService.updateAddress(update));

    assertEquals(ADDRESS_BOOK_NOT_FOUND, exception.getMessage());
  }

  @Test
  void updateAddressWithWrongOwnerThrows() {
    AddressBook update = new AddressBook();
    update.setId(200L);

    AddressBook existing = new AddressBook();
    existing.setId(200L);
    existing.setUserId(1L);

    when(mapper.selectById(200L)).thenReturn(existing);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(2L);

      AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
          () -> addressBookService.updateAddress(update));

      assertEquals(ADDRESS_BOOK_ACCESS_DENIED, exception.getMessage());
    }
  }

  @Test
  void setDefaultUpdatesRecords() {
    AddressBook input = new AddressBook();
    input.setId(300L);

    AddressBook existing = new AddressBook();
    existing.setId(300L);
    existing.setUserId(40L);

    when(mapper.selectById(300L)).thenReturn(existing);
    when(mapper.update(any(), any())).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(40L);

      addressBookService.setDefault(input);

      verify(mapper, times(2)).update(any(), any());
    }
  }

  @Test
  void getDefaultSuccess() {
    AddressBook address = new AddressBook();
    address.setId(400L);
    address.setUserId(50L);
    address.setIsDefault(1);

    when(mapper.selectOne(any())).thenReturn(address);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(50L);

      AddressBook result = addressBookService.getDefault();

      assertNotNull(result);
      assertEquals(400L, result.getId());
      verify(mapper).selectOne(wrapperCaptor.capture());
    }
  }

  @Test
  void getByIdWithCheckNotFoundThrows() {
    when(mapper.selectById(500L)).thenReturn(null);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(60L);

      AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
          () -> addressBookService.getByIdWithCheck(500L));

      assertEquals(ADDRESS_BOOK_NOT_FOUND, exception.getMessage());
    }
  }

  @Test
  void getByIdWithCheckAccessDeniedThrows() {
    AddressBook existing = new AddressBook();
    existing.setId(510L);
    existing.setUserId(61L);

    when(mapper.selectById(510L)).thenReturn(existing);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(62L);

      AddressBookBusinessException exception = assertThrows(AddressBookBusinessException.class,
          () -> addressBookService.getByIdWithCheck(510L));

      assertEquals(ADDRESS_BOOK_ACCESS_DENIED, exception.getMessage());
    }
  }

  @Test
  void getByIdWithCheckSuccess() {
    AddressBook existing = new AddressBook();
    existing.setId(520L);
    existing.setUserId(63L);

    when(mapper.selectById(520L)).thenReturn(existing);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(63L);

      AddressBook result = addressBookService.getByIdWithCheck(520L);

      assertNotNull(result);
      assertEquals(520L, result.getId());
    }
  }

  @Test
  void removeByIdWithCheckSuccess() {
    AddressBook existing = new AddressBook();
    existing.setId(600L);
    existing.setUserId(70L);

    when(mapper.selectById(600L)).thenReturn(existing);
    when(mapper.deleteById(600L)).thenReturn(1);

    try (MockedStatic<BaseContext> baseContext = mockStatic(BaseContext.class)) {
      baseContext.when(BaseContext::getCurrentId).thenReturn(70L);

      addressBookService.removeByIdWithCheck(600L);

      verify(mapper).deleteById(600L);
    }
  }
}
