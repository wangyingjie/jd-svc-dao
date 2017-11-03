package com.jd.svc.dao;

import com.jd.svc.domain.SvcBookingOrder;

import java.util.List;

/**
 * @author: wangyingjie1
 * @version: 1.0
 * @createdate: 2017-11-01 18:58
 */
public interface SvcBookingOrderDao {


    int insertSvcBookingOrder(SvcBookingOrder svcBookingOrder);

    List<SvcBookingOrder> getAllOrderByRootOrderId(Long rootOrderId);

    List<SvcBookingOrder> getNoDeliverOrderByRootOrderId(Long rootOrderId);

    List<SvcBookingOrder> getBookingOrderByBookingId(Long bookingId);


}
