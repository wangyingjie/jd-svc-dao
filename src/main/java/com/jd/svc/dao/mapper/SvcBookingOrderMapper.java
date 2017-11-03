package com.jd.svc.dao.mapper;


import com.jd.svc.domain.SvcBookingOrder;

import java.util.List;

/**
 * Created by wangyingjie1 on 2017/8/8.
 */
public interface SvcBookingOrderMapper {

    int insertSvcBookingOrder(SvcBookingOrder svcBookingOrder);

    int batchInsertSvcBookingOrder(List<SvcBookingOrder> svcBookingOrderList);

    List<SvcBookingOrder> getAllOrderByRootOrderId(Long rootOrderId);

    List<SvcBookingOrder> getNoDeliverOrderByRootOrderId(Long rootOrderId);

    List<SvcBookingOrder> getBookingOrderByBookingId(Long bookingId);

}
