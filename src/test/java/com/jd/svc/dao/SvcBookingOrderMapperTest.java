package com.jd.svc.dao;

import com.jd.svc.dao.mapper.SvcBookingOrderMapper;
import com.jd.svc.domain.SvcBookingOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-config.xml")
public class SvcBookingOrderMapperTest {

    @Autowired
    private SvcBookingOrderMapper svcBookingOrderMapper;

    @Test
    public void testInsertSvcBookingOrder() {

        SvcBookingOrder svcBookingOrder = new SvcBookingOrder();

        svcBookingOrder.setId(ThreadLocalRandom.current().nextLong(100L, 100000000L));
        svcBookingOrder.setErpOrderId(ThreadLocalRandom.current().nextLong(100L, 100000000L));
        svcBookingOrder.setSlave("true");

        int count = svcBookingOrderMapper.insertSvcBookingOrder(svcBookingOrder);

    }

    @Test
    public void testBatchInsertSvcBookingOrder() {

        List<SvcBookingOrder> svcBookingOrderList = new ArrayList<>();

        for (int i=0; i<5; i++) {
            SvcBookingOrder svcBookingOrder = new SvcBookingOrder();

            svcBookingOrder.setId(ThreadLocalRandom.current().nextLong(100L, 100000000L));
            svcBookingOrder.setErpOrderId(ThreadLocalRandom.current().nextLong(100L, 100000000L));
            svcBookingOrder.setSlave("true");
            svcBookingOrder.setFeatures("xxx" + i);

            svcBookingOrderList.add(svcBookingOrder);

        }

        int count = svcBookingOrderMapper.batchInsertSvcBookingOrder(svcBookingOrderList);

    }


}
