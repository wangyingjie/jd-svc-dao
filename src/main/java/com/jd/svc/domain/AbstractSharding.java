package com.jd.svc.domain;

/**
 * @author: wangyingjie1
 * @version: 1.0
 * @createdate: 2017-11-03 15:13
 */
public abstract class AbstractSharding {

    private String slave ;

    private boolean isMaster;

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }

    public String getSlave() {
        return slave;
    }

    public void setSlave(String slave) {
        this.slave = slave;
    }
}