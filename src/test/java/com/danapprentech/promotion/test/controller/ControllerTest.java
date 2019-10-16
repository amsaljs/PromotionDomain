package com.danapprentech.promotion.test.controller;

import com.danapprentech.promotion.models.Coupon;
import com.danapprentech.promotion.models.Couponhistory;
import com.danapprentech.promotion.models.Mcoupon;
import com.danapprentech.promotion.models.Redeemhistory;
import com.danapprentech.promotion.response.BaseResponse;
import com.danapprentech.promotion.response.CouponIssue;
import com.danapprentech.promotion.services.interfaces.ICouponHistoryService;
import com.danapprentech.promotion.services.interfaces.ICouponService;
import com.danapprentech.promotion.services.interfaces.IMasterCouponService;
import com.danapprentech.promotion.services.interfaces.IRedeemHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class ControllerTest extends AbstractTest{

    @Override
    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void getCouponListTest_Success() throws Exception {
        String url = "/promotion/all";
        MvcResult mvcResult = mvc.perform (MockMvcRequestBuilders.get (url)
                .accept (MediaType.APPLICATION_JSON_VALUE)).andReturn ();
        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        Coupon[] couponsList = super.mapFromJson(content, Coupon[].class);
        assertTrue(couponsList.length > 0);
    }

    @Test
    public void getCouponDetailTest_Success() throws Exception {


        String uri = "/promotion/detail/TCPN-07716c66-7fd5-45a3-8e98-44d1c79590a4";
        Coupon coupon = Coupon.builder ()
                        .memberId ("USR-01")
                        .build ();

        String inputJson = super.mapToJson (coupon);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);

        ObjectMapper mapper = new ObjectMapper ();
        CouponIssue couponIssue= mapper.convertValue (response.getData (),CouponIssue.class);

        assertTrue(couponIssue.getMemberId ().equalsIgnoreCase ("USR-01"));
    }

    @Test
    public void getCouponDetailTest_Empty() throws Exception {
        String uri = "/promotion/detail/01";
        Coupon coupon = Coupon.builder ()
                .memberId ("USR-01")
                .build ();

        String inputJson = super.mapToJson (coupon);

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);

        assertTrue(response.getMessage ().equalsIgnoreCase ("Coupon not found for this id :: 01"));
    }

    @Test
    public void getCouponDetailTest_Error() throws Exception {
        JSONObject jsonObject = new JSONObject ();
        String uri = "/promotion/detail/"+jsonObject;

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get(uri)
                .contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(500, status);
    }


    @Test
    public void getCouponRecommendationTest_Success() throws Exception {
        String uri = "/promotion/recommended";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("memberId","USR-02");
        jsonObject.put ("amount",25000);

        String inputJson = super.mapToJson (jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content (inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);

        ObjectMapper mapper = new ObjectMapper ();
        CouponIssue[] couponIssue= mapper.convertValue (response.getData (),CouponIssue[].class);
        assertTrue(couponIssue.length>0);
    }

    @Test
    public void getCouponRecommendationTest_Empty() throws Exception {
        String uri = "/promotion/recommended";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("memberId","USR-88");
        jsonObject.put ("amount",25000);

        String inputJson = super.mapToJson (jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content (inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);

        ObjectMapper mapper = new ObjectMapper ();
        CouponIssue[] couponIssue= mapper.convertValue (response.getData (),CouponIssue[].class);
        assertTrue(couponIssue.length==0);
    }

    @Test
    public void getCouponRecommendationTest_Error() throws Exception {
        String uri = "/promotion/recommended";
        JSONObject json = null;
        String inputJson = super.mapToJson (json);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content (inputJson)).andReturn();
        int status = mvcResult.getResponse().getStatus();
        assertEquals(500, status);
    }

    @Test
    public void createNewCouponTest_Success() throws Exception {
        String uri = "/promotion/create/coupon";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("memberId","USR-02");
        jsonObject.put ("amount",25000);
        String expected ="Success";

        String inputJson = super.mapToJson (jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content (inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value (), status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);

        assertTrue(expected.equalsIgnoreCase (response.getMessage ()));
    }

    @Test
    public void createNewCouponTest_Failed() throws Exception {
        String uri = "/promotion/create/coupon";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("memberId","USR-08");

        String inputJson = super.mapToJson (jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content (inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(HttpStatus.OK.value (), status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);

        assertTrue(response.getMessage ().equalsIgnoreCase ("Failed"));
    }

    @Test
    public void createNewCouponTest_Error() throws Exception {
        String uri = "/promotion/create/coupon";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(500, status);
    }

    @Test
    public void updateCouponStatusTest_Success() throws Exception {
        String uri = "/promotion/update/coupon";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("couponId","A1");
        jsonObject.put ("memberId","A1");
        jsonObject.put ("paymentMethodCode","000");
        jsonObject.put ("paymentId","A1");
        jsonObject.put ("couponAmount",12000);

        String inputJson = super.mapToJson(jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);
        assertEquals("Succeed",response.getMessage ());
    }

    @Test
    public void updateCouponStatusTest_Failed() throws Exception {
        String uri = "/promotion/update/coupon";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("couponId","TCPN-1094c2c6-499a-48c9-b3c0-c269a71a10a3");
        jsonObject.put ("memberId","USR-994facc9-2e40-47c2-840e-77680a90e033");
        jsonObject.put ("paymentMethodCode","000");
        jsonObject.put ("paymentId","cf7ec9ed-0614-49c2-bec9-ed0614b9c275");

        String inputJson = super.mapToJson(jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE).content(inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);
        assertEquals ("Failed",response.getMessage ());
    }

    @Test
    public void updateCouponStatusTest_Error() throws Exception {
        String uri = "/promotion/update/coupon";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.put(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(500, status);
    }

    @Test
    public void createCouponNewMember_Success() throws Exception {
        String uri = "/promotion/create/coupon/first";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("memberId","USR-Test11AC");
        jsonObject.put ("status","new member");

        String inputJson = super.mapToJson (jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content (inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);
        assertTrue (response.getMessage ().equalsIgnoreCase ("Success"));
    }

    @Test
    public void createCouponNewMember_Failed() throws Exception {
        String uri = "/promotion/create/coupon/first";
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put ("memberId","USR-Test1");
        jsonObject.put ("status","new member");

        String inputJson = super.mapToJson (jsonObject);
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content (inputJson)).andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(200, status);
        String content = mvcResult.getResponse().getContentAsString();
        BaseResponse response = super.mapFromJson(content, BaseResponse.class);
        assertTrue(response.getMessage ().equalsIgnoreCase ("Failed to save data"));
    }

    @Test
    public void createCouponNewMember_Error() throws Exception {
        String uri = "/promotion/create/coupon/first";

        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(uri)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        int status = mvcResult.getResponse().getStatus();
        assertEquals(500, status);
    }


}
