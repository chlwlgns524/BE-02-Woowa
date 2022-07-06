package com.example.woowa.customer.voucher.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.woowa.customer.customer.dto.CustomerAddressCreateRequest;
import com.example.woowa.customer.customer.dto.CustomerCreateRequest;
import com.example.woowa.customer.customer.dto.CustomerFindResponse;
import com.example.woowa.customer.customer.dto.CustomerGradeCreateRequest;
import com.example.woowa.customer.customer.repository.CustomerAddressRepository;
import com.example.woowa.customer.customer.repository.CustomerGradeRepository;
import com.example.woowa.customer.customer.repository.CustomerRepository;
import com.example.woowa.customer.customer.service.CustomerGradeService;
import com.example.woowa.customer.voucher.dto.VoucherCreateRequest;
import com.example.woowa.customer.voucher.dto.VoucherFindResponse;
import com.example.woowa.customer.voucher.enums.EventType;
import com.example.woowa.customer.voucher.enums.VoucherType;
import com.example.woowa.customer.voucher.repository.VoucherRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
class VoucherControllerTest {
  @Autowired
  MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CustomerGradeService customerGradeService;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private CustomerAddressRepository customerAddressRepository;

  @Autowired
  private CustomerGradeRepository customerGradeRepository;

  @Autowired
  private VoucherRepository voucherRepository;

  void makeDefaultCustomerGrade() {
    CustomerGradeCreateRequest customerGradeCreateRequest = new CustomerGradeCreateRequest(1, "일반", 3000, 2);
    customerGradeService.createCustomerGrade(customerGradeCreateRequest);
  }

  String createCustomer() throws Exception {
    CustomerAddressCreateRequest customerAddressCreateRequest = new CustomerAddressCreateRequest("서울특별시 동작구 상도동","빌라 101호","집");
    CustomerCreateRequest customerCreateRequest = new CustomerCreateRequest("dev12","Programmers123!", "2000-01-01", customerAddressCreateRequest);

    String body = mockMvc.perform(
        post("/api/v1/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(customerCreateRequest))
    ).andReturn().getResponse().getContentAsString();

    CustomerFindResponse customerFindResponse = objectMapper.readValue(body, CustomerFindResponse.class);
    return customerFindResponse.getLoginId();
  }

  @AfterEach
  void settingAfterTest() {
    customerGradeRepository.deleteAll();
    customerRepository.deleteAll();
    customerAddressRepository.deleteAll();
    voucherRepository.deleteAll();
  }

  @Test
  void registerMonthlyVoucher() throws Exception {
    makeDefaultCustomerGrade();
    String loginId = createCustomer();

    mockMvc.perform(
            get("/api/v1/vouchers/month/{loginId}", loginId)
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(document("vouchers-month",
            responseFields(
                fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("쿠폰 아이디"),
                fieldWithPath("[].voucherType").type(JsonFieldType.STRING).description("할인 타입"),
                fieldWithPath("[].eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                fieldWithPath("[].discountValue").type(JsonFieldType.NUMBER).description("할인 수치"),
                fieldWithPath("[].expirationDate").type(JsonFieldType.STRING).description("만료 시간"),
                fieldWithPath("[].code").type(JsonFieldType.STRING).description("등록 코드")
            )
        ));
  }

  @Test
  void registerVoucher() throws Exception {
    makeDefaultCustomerGrade();
    String loginId = createCustomer();
    VoucherCreateRequest voucherCreateRequest = new VoucherCreateRequest(VoucherType.FiXED.toString(),
        EventType.SPECIAL.toString(), 3000, "2022-12-01 12:00");
    String body = mockMvc.perform(
        post("/api/v1/vouchers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(voucherCreateRequest))
    ).andReturn().getResponse().getContentAsString();
    VoucherFindResponse voucherFindResponse = objectMapper.readValue(body, VoucherFindResponse.class);

    mockMvc.perform(
            get("/api/v1/vouchers/{loginId}/{id}", loginId, voucherFindResponse.getCode())
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(document("vouchers-register",
            responseFields(
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("쿠폰 아이디"),
                fieldWithPath("voucherType").type(JsonFieldType.STRING).description("할인 타입"),
                fieldWithPath("eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                fieldWithPath("discountValue").type(JsonFieldType.NUMBER).description("할인 수치"),
                fieldWithPath("expirationDate").type(JsonFieldType.STRING).description("만료 시간"),
                fieldWithPath("code").type(JsonFieldType.STRING).description("등록 코드")
            )
        ));
  }

  @Test
  void createVoucher() throws Exception {
    VoucherCreateRequest voucherCreateRequest = new VoucherCreateRequest(VoucherType.FiXED.toString(),
        EventType.SPECIAL.toString(), 3000, "2022-12-01 12:00");

    mockMvc.perform(
            post("/api/v1/vouchers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voucherCreateRequest))
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(document("vouchers-create",
            requestFields(
                fieldWithPath("voucherType").type(JsonFieldType.STRING).description("할인 타입"),
                fieldWithPath("eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                fieldWithPath("discountValue").type(JsonFieldType.NUMBER).description("할인 수치"),
                fieldWithPath("expirationDate").type(JsonFieldType.STRING).description("만료 시간")
                ),
            responseFields(
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("쿠폰 아이디"),
                fieldWithPath("voucherType").type(JsonFieldType.STRING).description("할인 타입"),
                fieldWithPath("eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                fieldWithPath("discountValue").type(JsonFieldType.NUMBER).description("할인 수치"),
                fieldWithPath("expirationDate").type(JsonFieldType.STRING).description("만료 시간"),
                fieldWithPath("code").type(JsonFieldType.STRING).description("등록 코드")
                )
        ));
  }

  @Test
  void findVoucher() throws Exception {
    VoucherCreateRequest voucherCreateRequest = new VoucherCreateRequest(VoucherType.FiXED.toString(),
        EventType.SPECIAL.toString(), 3000, "2022-12-01 12:00");

    String body = mockMvc.perform(
            post("/api/v1/vouchers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(voucherCreateRequest))
        ).andReturn().getResponse().getContentAsString();
    VoucherFindResponse voucherFindResponse = objectMapper.readValue(body, VoucherFindResponse.class);

    mockMvc.perform(
            get("/api/v1/vouchers/{id}", voucherFindResponse.getId())
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(document("vouchers-find",
            responseFields(
                fieldWithPath("id").type(JsonFieldType.NUMBER).description("쿠폰 아이디"),
                fieldWithPath("voucherType").type(JsonFieldType.STRING).description("할인 타입"),
                fieldWithPath("eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                fieldWithPath("discountValue").type(JsonFieldType.NUMBER).description("할인 수치"),
                fieldWithPath("expirationDate").type(JsonFieldType.STRING).description("만료 시간"),
                fieldWithPath("code").type(JsonFieldType.STRING).description("등록 코드")
            )
        ));
  }

  @Test
  void findUserVoucher() throws Exception {
    makeDefaultCustomerGrade();
    String loginId = createCustomer();

    mockMvc.perform(
            get("/api/v1/vouchers/month/{loginId}", loginId)
        );
    mockMvc.perform(
            get("/api/v1/vouchers/user/{loginId}", loginId)
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(document("vouchers-find-users",
            responseFields(
                fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("쿠폰 아이디"),
                fieldWithPath("[].voucherType").type(JsonFieldType.STRING).description("할인 타입"),
                fieldWithPath("[].eventType").type(JsonFieldType.STRING).description("이벤트 타입"),
                fieldWithPath("[].discountValue").type(JsonFieldType.NUMBER).description("할인 수치"),
                fieldWithPath("[].expirationDate").type(JsonFieldType.STRING).description("만료 시간"),
                fieldWithPath("[].code").type(JsonFieldType.STRING).description("등록 코드")
            )
        ));
  }

  @Test
  void deleteVoucher() throws Exception {
    makeDefaultCustomerGrade();
    String loginId = createCustomer();
    String body = mockMvc.perform(
            get("/api/v1/vouchers/month/{loginId}", loginId)
        ).andReturn().getResponse().getContentAsString();
    List<VoucherFindResponse> result = objectMapper.readValue(body, new TypeReference<List<VoucherFindResponse>>() {});
    mockMvc.perform(
            delete("/api/v1/vouchers/{loginId}/{id}", loginId,result.get(0).getId())
        )
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(document("vouchers-delete",
            responseBody()
        ));
  }
}