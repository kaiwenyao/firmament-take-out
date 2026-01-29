package dev.kaiwen.controller.user;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kaiwen.constant.JwtClaimsConstant;
import dev.kaiwen.entity.AddressBook;
import dev.kaiwen.properties.JwtProperties;
import dev.kaiwen.service.AddressBookService;
import dev.kaiwen.utils.JwtService;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AddressBookController.class)
class AddressBookControllerTest {

  @MockitoBean
  private AddressBookService addressBookService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private JwtProperties jwtProperties;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  private void setupUserJwtMock() {
    given(jwtProperties.getUserTokenName()).willReturn("token");
    given(jwtProperties.getUserSecretKey()).willReturn("mock-secret-key");
    Claims claims = mock(Claims.class);
    given(jwtService.parseJwt(eq("mock-secret-key"),
        org.mockito.ArgumentMatchers.anyString())).willReturn(claims);
    given(claims.get(JwtClaimsConstant.USER_ID)).willReturn("1");
  }

  @Test
  void listSuccess() throws Exception {
    setupUserJwtMock();

    List<AddressBook> list = Collections.singletonList(
        AddressBook.builder().id(1L).consignee("张三").phone("13800138000").build());
    given(addressBookService.list()).willReturn(list);

    mockMvc.perform(get("/user/addressBook/list").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].id").value(1))
        .andExpect(jsonPath("$.data[0].consignee").value("张三"));

    verify(addressBookService).list();
  }

  @Test
  void saveSuccess() throws Exception {
    setupUserJwtMock();

    AddressBook addressBook = AddressBook.builder()
        .consignee("李四")
        .phone("13900139000")
        .sex("1")
        .detail("测试地址")
        .build();

    mockMvc.perform(post("/user/addressBook")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(addressBook)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.msg").value(nullValue()));

    verify(addressBookService).saveAddress(any(AddressBook.class));
  }

  @Test
  void getByIdSuccess() throws Exception {
    setupUserJwtMock();

    Long id = 100L;
    AddressBook addressBook = AddressBook.builder().id(id).consignee("王五").phone("13700137000")
        .build();
    given(addressBookService.getByIdWithCheck(id)).willReturn(addressBook);

    mockMvc.perform(get("/user/addressBook/{id}", id).header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.id").value(id))
        .andExpect(jsonPath("$.data.consignee").value("王五"));

    verify(addressBookService).getByIdWithCheck(id);
  }

  @Test
  void updateSuccess() throws Exception {
    setupUserJwtMock();

    AddressBook addressBook = AddressBook.builder().id(1L).consignee("赵六").phone("13600136000")
        .build();

    mockMvc.perform(put("/user/addressBook")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(addressBook)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(addressBookService).updateAddress(any(AddressBook.class));
  }

  @Test
  void setDefaultSuccess() throws Exception {
    setupUserJwtMock();

    AddressBook addressBook = AddressBook.builder().id(1L).build();

    mockMvc.perform(put("/user/addressBook/default")
            .header("token", "mock-accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(addressBook)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(addressBookService).setDefault(any(AddressBook.class));
  }

  @Test
  void deleteByIdSuccess() throws Exception {
    setupUserJwtMock();

    Long id = 1L;

    mockMvc.perform(delete("/user/addressBook").param("id", String.valueOf(id))
            .header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1));

    verify(addressBookService).removeByIdWithCheck(id);
  }

  @Test
  void getDefaultSuccess() throws Exception {
    setupUserJwtMock();

    AddressBook defaultAddr = AddressBook.builder().id(1L).consignee("默认").isDefault(1).build();
    given(addressBookService.getDefault()).willReturn(defaultAddr);

    mockMvc.perform(get("/user/addressBook/default").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1))
        .andExpect(jsonPath("$.data.consignee").value("默认"));

    verify(addressBookService).getDefault();
  }

  @Test
  void getDefaultWhenNull() throws Exception {
    setupUserJwtMock();

    given(addressBookService.getDefault()).willReturn(null);

    mockMvc.perform(get("/user/addressBook/default").header("token", "mock-accessToken"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.msg").value("没有查询到默认地址"));

    verify(addressBookService).getDefault();
  }
}
