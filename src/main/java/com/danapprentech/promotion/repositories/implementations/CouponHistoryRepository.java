package com.danapprentech.promotion.repositories.implementations;

import com.danapprentech.promotion.models.Couponhistory;
import com.danapprentech.promotion.repositories.interfaces.ICouponHistoryRepository;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.UUID;

@Repository
@Transactional
public class CouponHistoryRepository implements ICouponHistoryRepository {
    private static final Logger logger = LoggerFactory.getLogger(CouponHistoryRepository.class);
    private EntityManagerFactory emf;
    @Autowired
    public CouponHistoryRepository(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Couponhistory getDataByPaymentId(String paymentId) {
        Couponhistory couponhistory = null;
        EntityManager em = getEntityManager ();
        em.getTransaction ().begin ();
        logger.info ("Entity manager {}",em);
        try {
            String sql = "from Couponhistory where payment_id = '"+paymentId+"'";

            couponhistory = em.createQuery (sql, Couponhistory.class)
                    .setLockMode (LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult ();
            em.getTransaction ().commit ();
        }catch (Exception e){
            em.getTransaction ().rollback ();
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
        }
        em.close ();
        return couponhistory;
    }

    @Override
    public String addHistory(JSONObject jsonObject) {
        EntityManager em = getEntityManager ();
        em.getTransaction ().begin ();
        int saveCount = 0;
        String uniqueId = "MCPN-";
        uniqueId += UUID.randomUUID().toString();
        logger.info ("Entity manager {}",em);
        String response ="Failed";

        try {
            String paymentId = (String) jsonObject.get ("paymentId");
            String couponId = (String) jsonObject.get ("couponId");
            String memberId = (String) jsonObject.get ("memberId");

            String sql = "INSERT into Couponhistory (couponhistory_id,payment_id," +
                    "coupon_id, member_id)" +
                    "values(?,?,?,?)";

            saveCount = em.createNativeQuery (sql)
                    .setParameter (1,uniqueId)
                    .setParameter (2,paymentId)
                    .setParameter (3,couponId)
                    .setParameter (4,memberId)
                    .executeUpdate ();
            em.getTransaction ().commit ();
            if(saveCount != 0){
                response = "Success";
            }else {
                response = "Failed";
            }
        }catch (Exception e){
            em.getTransaction ().rollback ();
            logger.warn ("Error: {} - {}",e.getMessage (),e.getStackTrace ());
            response = "Failed";
        }
        em.close ();
        return response;
    }

    private EntityManager getEntityManager(){
        return emf.createEntityManager ();
    }
}