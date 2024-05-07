package cn.iocoder.yudao.module.crm.service.callcenter;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo.CrmCallcenterCallReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo.CrmCallcenterUserRespVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.callcenter.CrmCallcenterUserDO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;


/**
 * 外各 Service 接口
 *
 * @author fhqsuhpv
 */
public interface CrmCallCenterService {


    /**
     * 向外呼叫
     *
     * @param callReqVO
     * @param userId
     * @return
     */
    CommonResult<ResponseEntity<String>> call(CrmCallcenterCallReqVO callReqVO, Long userId);

    /**
     * 获取外呼用户
     *
     * @param phone
     * @return
     */
    CrmCallcenterUserDO getCallCenterUser(String phone);

    /**
     * 判断是否为外呼用户
     *
     * @param phone
     * @return
     */
    Boolean isCallCenterUser(String phone);

    HttpHeaders getHeaders(String partnerId);

    CrmCallcenterUserRespVO bindingUser(Long userId, String phone);


}
