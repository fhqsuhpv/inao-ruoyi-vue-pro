package cn.iocoder.yudao.module.crm.service.callcenter;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo.CrmCallcenterCallReqVO;

import cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo.CrmCallcenterUserRespVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.callcenter.CrmCallcenterConfigDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.callcenter.CrmCallcenterUserDO;
import cn.iocoder.yudao.module.crm.dal.mysql.callcenter.CrmCallcenterConfigMapper;
import cn.iocoder.yudao.module.crm.dal.mysql.callcenter.CrmCallcenterUserMapper;
import cn.iocoder.yudao.module.crm.dal.mysql.clue.CrmClueMapper;
import cn.iocoder.yudao.module.crm.dal.mysql.customer.CrmCustomerMapper;
import cn.iocoder.yudao.module.crm.enums.callcenter.CrmCallCenterCallTypeEnum;
import cn.iocoder.yudao.module.crm.enums.callcenter.CrmCallCenterManufacturerTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;
import java.util.*;
import static cn.hutool.crypto.SecureUtil.md5;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.crm.enums.ErrorCodeConstants.*;

/**
 * 外呼 Service 实现类
 *
 * @author fhqsuhpv
 */
@Service
@Validated
public class CrmCallCenterServiceImpl implements CrmCallCenterService {

    @Resource
    private CrmCallcenterConfigMapper callcenterConfigMapper;

    @Resource
    private CrmCallcenterUserMapper callcenterUserMapper;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private CrmClueMapper clueMapper;

    @Resource
    private CrmCustomerMapper customerMapper;

    private HttpHeaders getHeaders(CrmCallcenterConfigDO configDO, Map<String,Object> param,String partnerId) {
        //云客接口部分用管理员ID外呼接口需要使用UserId
        String pid = partnerId != null ? partnerId : configDO.getAdminCode();//管理员ID
        String api_key = configDO.getApiKey();  //接口签名KEY
        String company = configDO.getCompanyCode();  //企业串码

        //时间戳
        String tiemstamp = String.valueOf(System.currentTimeMillis());

        HttpHeaders headers = new HttpHeaders();

        if (configDO.getManufacturerId()==CrmCallCenterManufacturerTypeEnum.YUNKE.getType()){

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("partnerId", pid);//管理员ID
            headers.add("company", company);//企业串码
            headers.add("timestamp", tiemstamp);
            headers.add("sign", md5(api_key + company + pid + tiemstamp).toUpperCase());//签名

        }else if (configDO.getManufacturerId()==CrmCallCenterManufacturerTypeEnum.LIANLIAN.getType()) {

            headers.setContentType((MediaType.parseMediaType("application/json;charset=UTF-8")));
            headers.add("api_key", configDO.getApiKey());
            headers.add("sign", md5(param + configDO.getSecretKey()));
        }
        System.out.println("headers------->"+headers);
        return headers;
    }

    @Override
    public CommonResult<ResponseEntity<String>> call(CrmCallcenterCallReqVO callReqVO, Long userId) {

        CrmCallcenterConfigDO configDO = callcenterConfigMapper.selectByManufactoryId(callReqVO.getManufacturerId());
        System.out.println("configDO------->"+configDO);
        if (configDO == null) throw exception(CRM_CALLCENTER_CONFIG_NOT_EXISTS);

        CrmCallcenterUserDO userDO = callcenterUserMapper.selectOne("user_id",userId);
        System.out.println("userDO------->"+userDO);
        if (userDO == null) throw exception(CRM_CALLCENTER_USER_NOT_EXISTS);

        String callphone = null;
        if(callReqVO.getCallType() == CrmCallCenterCallTypeEnum.CUSTOMER.getType())
            callphone = customerMapper.selectById(callReqVO.getCallId()).getMobile();
        else if(callReqVO.getCallType() == CrmCallCenterCallTypeEnum.CLUE.getType()){
            System.out.println(clueMapper.selectById(callReqVO.getCallId()));
            callphone =clueMapper.selectById(callReqVO.getCallId()).getMobile();}
        System.out.println("callphone------->"+callphone);

        if (callphone == null) throw exception(CRM_CALLCENTER_CALLPHONE_NOT_EXISTS);

        //云客调用呼叫接口调用
        if(configDO.getManufacturerId() == CrmCallCenterManufacturerTypeEnum.YUNKE.getType()){
            Map<String,Object> param = new HashMap<>();
            param.put("phone",callphone);
            param.put("callModel","1");
            HttpHeaders headers = getHeaders(configDO,null,userDO.getYunkeCallcenterUserId());
            HttpEntity<String> request = new HttpEntity<>(JSONUtil.toJsonStr(param), headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange("https://phone.yunkecn.com/open/call/partnerCustomer", HttpMethod.POST, request, String.class);
            System.out.println("response------->"+responseEntity);
            return success(responseEntity);
        //连连调用呼叫接口调用
        }else if(configDO.getManufacturerId() == CrmCallCenterManufacturerTypeEnum.LIANLIAN.getType()){
            List<String> callerNoList = new ArrayList<>();
            callerNoList.add(userDO.getLianlianCallcenterPhone());
            Map<String,Object> param = new HashMap<>();
            param.put("calledNo",callphone);
            param.put("callerId","");
            param.put("ts",String.valueOf(System.currentTimeMillis() / 1000));
            param.put("callerNos",callerNoList);
            param.put("type","TYC");
            HttpHeaders headers = getHeaders(configDO,param,null);
            HttpEntity<String> requestEntity = new HttpEntity<>(param.toString(),headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange("https://api.51lianlian.cn/api/call/bind", HttpMethod.POST, requestEntity, String.class);
            System.out.println("response------->"+responseEntity);
            JSONObject jsonObject = JSONUtil.parseObj(responseEntity.getBody());
            if(jsonObject.get("success") != "true") throw exception(CRM_CALLCENTER_CALL_FAIL);
            return success(responseEntity);
        }
        throw exception(CRM_CALLCENTER_MANUFA_NOT_EXISTS);
    }

    @Override
    public CrmCallcenterUserDO getCallCenterUser(String phone) {
        return callcenterUserMapper.selectOne(new LambdaQueryWrapperX<CrmCallcenterUserDO>().eq(CrmCallcenterUserDO::getLianlianCallcenterPhone,phone));
    }

    public HttpHeaders getHeaders(String partnerId) {
        //云客接口部分用管理员ID外呼接口需要使用UserId
        String pid = partnerId != null ? partnerId : "p445B194DD03B47B893BD3256A57721DE";//管理员ID
        String api_key = "A1C95A823C8B448B9578B4";  //接口签名KEY
        String company = "7fkuu0";  //企业串码

        String tiemstamp = String.valueOf(System.currentTimeMillis());

        String s = md5(api_key + company + pid + tiemstamp).toUpperCase();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("partnerId", pid);//管理员ID
        headers.add("company", company);//企业串码
        headers.add("timestamp", tiemstamp);
        headers.add("sign", s);//签名
        System.out.println("headers------->"+headers);
        System.out.println("sign------->"+(api_key + company + pid + tiemstamp));
        System.out.println("sign_md5------->"+md5(api_key + company + pid + tiemstamp).toUpperCase());
        return headers;
    }

    @Override
    public Boolean isCallCenterUser(String phone) {
        Map<String,Object> param = new HashMap<>();
        param.put("phone",phone);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONUtil.toJsonStr(param),getHeaders(null));
        ResponseEntity<String> responseEntity = restTemplate.exchange("https://phone.yunkecn.com/open/user/getUserByPhone", HttpMethod.POST, requestEntity, String.class);
        System.out.println("responseEntity------->"+responseEntity);
        responseEntity.getBody();
        return true;
    }

    @Override
    public CrmCallcenterUserRespVO bindingUser(Long userId, String phone) {
        isCallCenterUser(phone);

        Map<String,Object> param = new HashMap<>();
        param.put("phone",phone);
        param.put("userId",userId);

        HttpEntity<String> requestEntity = new HttpEntity<>(JSONUtil.toJsonStr(param),getHeaders(null));
        ResponseEntity<String> responseEntity = restTemplate.exchange("https://phone.yunkecn.com/open/user/phonePass", HttpMethod.POST, requestEntity, String.class);
        System.out.println("responseEntity------->"+responseEntity);
        return null;
    }

}
