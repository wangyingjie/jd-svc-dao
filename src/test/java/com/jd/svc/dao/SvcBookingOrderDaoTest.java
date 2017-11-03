package com.jd.svc.dao;


import com.jd.svc.domain.SvcBookingOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.ThreadLocalRandom;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-config.xml")
public class SvcBookingOrderDaoTest {

    @Autowired
    private SvcBookingOrderDao svcBookingOrderDao;

    @Test
    public void testInsertSvcBookingOrder() {

        SvcBookingOrder svcBookingOrder = new SvcBookingOrder();

        svcBookingOrder.setId(ThreadLocalRandom.current().nextLong(100L, 100000000L));
        svcBookingOrder.setErpOrderId(ThreadLocalRandom.current().nextLong(100L, 100000000L));

        int count = svcBookingOrderDao.insertSvcBookingOrder(svcBookingOrder);

    }

}
