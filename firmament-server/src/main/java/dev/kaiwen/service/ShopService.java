package dev.kaiwen.service;

/**
 * 店铺服务接口
 */
public interface ShopService {

    /**
     * 设置店铺营业状态
     * @param status 店铺状态，1表示营业中，0表示打烊中
     */
    void setStatus(Integer status);

    /**
     * 获取店铺营业状态
     * @return 店铺状态，1表示营业中，0表示打烊中
     */
    Integer getStatus();
}
