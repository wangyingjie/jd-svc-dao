package com.jd.svc.domain;

import java.util.Date;

/**
 * @author: wangyingjie1
 * @version: 1.0
 * @createdate: 2017-11-01 15:47
 */
public class SvcBookingOrder extends AbstractSharding {

    private Long id;

    private Long bookingId;

    private Long rootOrderId;

    private Long directOrderId;

    private Long erpOrderId;

    private String skuInfo;

    private Double orderFee;

    private Integer deliverStatus;

    private Integer orbStatus;

    private Integer afsStatus;

    private Date generatedTime;

    private String features;

    private Date created;

    private Date modified;

    private Integer yn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getRootOrderId() {
        return rootOrderId;
    }

    public void setRootOrderId(Long rootOrderId) {
        this.rootOrderId = rootOrderId;
    }

    public Long getDirectOrderId() {
        return directOrderId;
    }

    public void setDirectOrderId(Long directOrderId) {
        this.directOrderId = directOrderId;
    }

    public Long getErpOrderId() {
        return erpOrderId;
    }

    public void setErpOrderId(Long erpOrderId) {
        this.erpOrderId = erpOrderId;
    }

    public String getSkuInfo() {
        return skuInfo;
    }

    public void setSkuInfo(String skuInfo) {
        this.skuInfo = skuInfo == null ? null : skuInfo.trim();
    }

    public Double getOrderFee() {
        return orderFee;
    }

    public void setOrderFee(Double orderFee) {
        this.orderFee = orderFee;
    }

    public Integer getDeliverStatus() {
        return deliverStatus;
    }

    public void setDeliverStatus(Integer deliverStatus) {
        this.deliverStatus = deliverStatus;
    }

    public Integer getOrbStatus() {
        return orbStatus;
    }

    public void setOrbStatus(Integer orbStatus) {
        this.orbStatus = orbStatus;
    }

    public Integer getAfsStatus() {
        return afsStatus;
    }

    public void setAfsStatus(Integer afsStatus) {
        this.afsStatus = afsStatus;
    }

    public Date getGeneratedTime() {
        return generatedTime;
    }

    public void setGeneratedTime(Date generatedTime) {
        this.generatedTime = generatedTime;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features == null ? null : features.trim();
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public Integer getYn() {
        return yn;
    }

    public void setYn(Integer yn) {
        this.yn = yn;
    }
}