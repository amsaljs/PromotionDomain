package com.danapprentech.promotion.services.implementations;

import com.danapprentech.promotion.broker.Producer;
import com.danapprentech.promotion.models.Coupon;
import com.danapprentech.promotion.models.Couponhistory;
import com.danapprentech.promotion.models.Mcoupon;
import com.danapprentech.promotion.models.Redeemhistory;
import com.danapprentech.promotion.repositories.CouponRepo;
import com.danapprentech.promotion.repositories.GenerateCouponHistoryRepo;
import com.danapprentech.promotion.repositories.MasterRepo;
import com.danapprentech.promotion.repositories.RedeemHistoryRepo;
import com.danapprentech.promotion.response.CouponDetail;
import com.danapprentech.promotion.response.CouponIssue;
import com.danapprentech.promotion.services.interfaces.ICouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CouponService implements ICouponService {
    private static final Logger logger = LoggerFactory.getLogger(CouponService.class);
    @Autowired
    private CouponRepo couponRepo;
    @Autowired
    private MasterRepo masterRepo;
    @Autowired
    private GenerateCouponHistoryRepo generateCouponHistoryRepo;
    @Autowired
    private RedeemHistoryRepo redeemHistoryRepo;

    private Producer producer = new Producer ("queue.promotion.data");

    @Override
    @Transactional
    public CouponIssue getCouponDetailsById(String couponID) {
        CouponIssue couponIssue = null;
        try {
            Coupon coupon = couponRepo.findAllByCouponId (couponID);
            Mcoupon mcoupon = masterRepo.findAllByMCouponId (coupon.getMCouponId ());
            couponIssue = new CouponIssue.CouponIssuebuilder ()
                    .withCouponId (coupon.getCouponId ())
                    .withMemberId (coupon.getMemberId ())
                    .withCouponName (mcoupon.getmCouponDescription ())
                    .withCouponAmount (mcoupon.getmCouponAmount ())
                    .withPaymentMethod (mcoupon.getPaymentMethod ())
                    .withCouponStatus (coupon.getCouponStatus ())
                    .withCouponExpired (coupon.getCouponExpired ())
                    .build ();
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return couponIssue;
    }

    @Override
    @Transactional
    public List<Coupon> getAllCoupons() {

        return couponRepo.findAll ();
    }

    @Override
    @Transactional
    public List<CouponIssue> getCouponRecommendation(JSONObject jsonObject) {
        ArrayList<Mcoupon> mcouponList = new ArrayList<Mcoupon> ();
        ArrayList<CouponIssue> couponIssueList = new ArrayList<CouponIssue> ();
        List<CouponIssue> list = new ArrayList<CouponIssue> ();
        try {
            String time = new SimpleDateFormat ("yyyy-MM-dd").format(new Date ());
            String memberId = (String)jsonObject.get ("memberId");
            List<Coupon> couponList = couponRepo.getRecommendation (memberId,time);

            Number number = (Number) jsonObject.get ("amount");
            Long value = number.longValue ();

            for (Coupon coupon: couponList) {
                mcouponList.add (masterRepo.findAllByMCouponIdAndAndMCouponAmount (coupon.getMCouponId (),value));
            }
            for(int i=0; i<couponList.size (); i++){
                CouponIssue couponIssue = new CouponIssue.CouponIssuebuilder ()
                        .withCouponId (couponList.get (i).getCouponId ())
                        .withMemberId (couponList.get (i).getMemberId ())
                        .withCouponAmount (mcouponList.get (i).getmCouponAmount ())
                        .withCouponName (mcouponList.get (i).getmCouponDescription ())
                        .withPaymentMethod (mcouponList.get (i).getPaymentMethod ())
                        .withCouponExpired (couponList.get (i).getCouponExpired ())
                        .withCouponStatus (couponList.get (i).getCouponStatus ())
                        .build ();

                couponIssueList.add (couponIssue);
            }
            Collections.sort (couponIssueList, new Comparator<CouponIssue> () {
                @Override
                public int compare(CouponIssue o1, CouponIssue o2) {
                    return (int) (o1.getCouponAmount () - o2.getCouponAmount ());
                }
            });
            Collections.reverse (couponIssueList);
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return couponIssueList;
    }

    @Override
    @Transactional
    public String saveOrUpdateCoupon(JSONObject jsonObject) {
        String getResponse = "Failed";
        int returnValue = 0;
        try{
            Number number = (Number) jsonObject.get ("amount");
            Long value = number.longValue ();
            List<Mcoupon> mcoupons = masterRepo.checkMinimumTransaction (value);

            String uniqueId = "TCPN-"+ UUID.randomUUID().toString();
            String memberId = (String) jsonObject.get ("memberId");
            String masterId = mcoupons.get (0).getmCouponId ();
            String time = LocalDate.now ().plusDays (20).toString ();

            returnValue = couponRepo.insertCoupon (uniqueId,masterId,memberId,time);

            if(returnValue == 1){
                getResponse = "Success";
                CouponDetail couponDetail = getCouponDetailCoupon (uniqueId);
                ObjectMapper mapper = new ObjectMapper ();
                String couponString = mapper.writeValueAsString (couponDetail);
                JSONParser parser = new JSONParser ();
                JSONObject object = (JSONObject) parser.parse (couponString);
                object.put ("type","CREATE");
                producer.sendToExchange (object.toJSONString ());
                String historyId = "RHCPN-"+ UUID.randomUUID().toString();
                Couponhistory history = Couponhistory.builder ()
                        .couponhistoryId (historyId)
                        .couponId (uniqueId)
                        .memberId ((String)jsonObject.get ("memberId"))
                        .paymentId ((String)jsonObject.get ("paymentId"))
                        .build ();
                generateCouponHistoryRepo.save (history);
            }
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return getResponse;
    }

    @Override
    @Transactional
    public Integer updateStatus(JSONObject jsonObject) {
        int rows =0;
        Redeemhistory redeemhistory = null;
        try{
            String couponID = (String) jsonObject.get ("couponId");
            String paymentCode = (String) jsonObject.get ("paymentMethodCode");
            String paymentId = (String) jsonObject.get ("paymentId");
            Integer amount = (Integer) jsonObject.get ("couponAmount");
            String memberId = (String) jsonObject.get ("memberId");
            String uniqueId = "RCPN-"+ UUID.randomUUID().toString();
            LocalDateTime time = LocalDateTime.now ();
            Redeemhistory redeem = Redeemhistory.builder ()
                    .idRedeem (uniqueId)
                    .couponId (couponID)
                    .memberId (memberId)
                    .paymentId (paymentId)
                    .build ();

            redeemhistory = redeemHistoryRepo.findAllByPaymentId (paymentId);
            if(redeemhistory == null){
                logger.info ("Redeem history null");
                CouponIssue couponIssue = getCouponDetailsById (couponID);
                if(!couponIssue.getPaymentMethod ().equalsIgnoreCase ("000")){
                    logger.info ("payment method code doesn't 000");
                    if(paymentCode.equalsIgnoreCase (couponIssue.getPaymentMethod ())){
                        logger.info ("payment method same to data");
                        if(amount.longValue () == couponIssue.getCouponAmount ()){
                            logger.info ("check amount is pass");
                            rows = couponRepo.updateCouponStatus (couponID,memberId,time);
                            if(rows ==1){
                                logger.info ("update coupon status success");
                                Redeemhistory obj = redeemHistoryRepo.save (redeem);
                                updateCouponStatusInDataDomain(jsonObject);
                            }
                        }
                    }
                }else{
                    logger.info ("payment method is 000");
                    if(amount.longValue () == couponIssue.getCouponAmount ()){
                        logger.info ("check amount is pass");
                        rows = couponRepo.updateCouponStatus (couponID,memberId,time);
                        if(rows ==1){
                            logger.info ("update coupon status success");
                            Redeemhistory obj = redeemHistoryRepo.save (redeem);
                            updateCouponStatusInDataDomain(jsonObject);
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return rows;
    }

    @Override
    @Transactional
    public Integer updateStatusTrue(JSONObject jsonObject) {
        int response = 0;
        try {
            String couponId = (String) jsonObject.get ("couponId");
            LocalDateTime time = LocalDateTime.now ();
            response = couponRepo.rollbackCouponStatus (couponId,time);
            if(response == 1){
                updateCouponStatusInDataDomain(jsonObject);
            }
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return response;
    }

    @Override
    @Transactional
    public Integer firstCoupon(JSONObject jsonObject) {
        List<Coupon> coupon =null;
        int returnValue = 0;
        try {
            String desc = (String) jsonObject.get ("status");
            Mcoupon mcoupon = masterRepo.findAllByMCouponDescription (desc);
            String memberID = (String) jsonObject.get ("memberId");
            String masterId = mcoupon.getmCouponId ();
            String uniqueId = "TCPN-"+ UUID.randomUUID().toString();
            String time = LocalDateTime.now ().plusDays (10).toString ();
            coupon = checkForNewMember (memberID,mcoupon.getmCouponId ());
            if(coupon.isEmpty ()){
                returnValue = couponRepo.insertCoupon (uniqueId,masterId,memberID,time);
                if(returnValue == 1){
                    logger.info ("insert data coupon success");
                    CouponDetail couponDetail = getCouponDetailCoupon (uniqueId);
                    ObjectMapper mapper = new ObjectMapper ();
                    String couponString = mapper.writeValueAsString (couponDetail);
                    JSONParser parser = new JSONParser ();
                    JSONObject object = (JSONObject) parser.parse (couponString);
                    object.put ("type","CREATE");
                    producer.sendToExchange (object.toJSONString ());
                }
            }
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return returnValue;
    }

    @Override
    @Transactional
    public List<Coupon> checkForNewMember(String memberId, String mCouponId) {
        return couponRepo.checkExitNewMember (memberId,mCouponId);
    }

    @Override
    @Transactional
    public Integer deleteById(String couponId) {
        return couponRepo.deleteAllByCouponId (couponId);
    }

    public CouponDetail getCouponDetailCoupon(String couponID) {
        CouponDetail couponDetail = null;
        try {
            Coupon coupon = couponRepo.findAllByCouponId (couponID);
            Mcoupon mcoupon = masterRepo.findAllByMCouponId (coupon.getMCouponId ());
            couponDetail = CouponDetail.builder ()
                    .couponId (coupon.getCouponId ())
                    .memberId (coupon.getMemberId ())
                    .couponName (mcoupon.getmCouponDescription ())
                    .couponAmount (mcoupon.getmCouponAmount ())
                    .couponExpired (coupon.getCouponExpired ())
                    .couponStatus (coupon.getCouponStatus ())
                    .paymentMethod (mcoupon.getPaymentMethod ())
                    .createdAt (coupon.getCreateTime ())
                    .updatedAt (coupon.getUpdateTime ())
                    .build ();
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return couponDetail;
    }

    public String updateCouponStatusInDataDomain(JSONObject jsonObject){
        try{
            String couponID = (String) jsonObject.get ("couponId");
            CouponIssue couponIssue = getCouponDetailsById (couponID);
            Coupon coupon = couponRepo.findAllByCouponId (couponID);
            JSONObject json = new JSONObject();
            json.put ("couponId",couponIssue.getCouponId ());
            json.put("couponStatus",couponIssue.getCouponStatus ());
            json.put ("updatedAt",coupon.getUpdateTime ().toString ());
            json.put ("type","UPDATE");
            producer.sendToExchange (json.toString ());
        }catch (Exception e){
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        return "SUCCEED";
    }

}
