package com.jd.svc.dao.impl;

import com.jd.cobarclient.mybatis.spring.MySqlSessionTemplate;
import com.jd.svc.dao.SvcBookingOrderDao;
import com.jd.svc.domain.SvcBookingOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: wangyingjie1
 * @version: 1.0
 * @createdate: 2017-11-01 18:58
 */
@Repository("svcBookingOrderDao")
public class SvcBookingOrderDaoImpl implements SvcBookingOrderDao {

    private static final String NAME_SPACE = "com.jd.svc.dao.mapper.SvcBookingOrderMapper.";

    @Autowired
    private MySqlSessionTemplate sqlSessionTemplate;

    @Override
    public int insertSvcBookingOrder(SvcBookingOrder svcBookingOrder) {

        return sqlSessionTemplate.insert(NAME_SPACE + "insertSvcBookingOrder", svcBookingOrder);
    }

    @Override
    public List<SvcBookingOrder> getAllOrderByRootOrderId(Long rootOrderId) {
        return null;
    }

    @Override
    public List<SvcBookingOrder> getNoDeliverOrderByRootOrderId(Long rootOrderId) {
        return null;
    }

    @Override
    public List<SvcBookingOrder> getBookingOrderByBookingId(Long bookingId) {
        return null;
    }
}