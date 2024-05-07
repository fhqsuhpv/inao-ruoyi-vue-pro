package cn.iocoder.yudao.module.crm.controller.admin.callcenter;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo.CrmCallcenterCallReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo.CrmCallcenterUserRespVO;
import cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo.CrmCallcenterUserSaveReqVO;
import cn.iocoder.yudao.module.crm.service.callcenter.CrmCallCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static cn.hutool.crypto.SecureUtil.md5;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - CRM 外呼系统")
@RestController
@RequestMapping("/crm/call-center")
@Validated
public class CrmCallCenterController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Resource
    private CrmCallCenterService crmCallCenterService;

//    @PostMapping("/call")
//    @Operation(summary = "打电话外呼")
//    @PreAuthorize("@ss.hasPermission('crm:callcenter:call')")
//    public CommonResult<Boolean> call()  {
//        List<String> callerNoList = new ArrayList<>();
//        callerNoList.add("13269727237");
//        Map<String,Object> param = new HashMap<>();
//        param.put("calledNo","17701305311");
//        param.put("callerId","00004");
//        param.put("ts",String.valueOf(System.currentTimeMillis() / 1000));
//        param.put("callerNos",callerNoList);
//        param.put("type","TYC");
//        //管理员ID
//        String s = param + "a5c2096f3d8be59d58ccee2136b00948";
//        String sign = MD5.stringToMD5(s);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType((MediaType.parseMediaType("application/json;charset=UTF-8")));
//        headers.add("api_key", "4967620847861813");
//        headers.add("sign", sign);
//        HttpEntity<String> requestEntity = new HttpEntity<>(param.toString(),headers);
//        ResponseEntity<String> responseEntity = restTemplate.exchange("https://api.51lianlian.cn/api/call/bind", HttpMethod.POST, requestEntity, String.class);
//        System.out.println("responseEntity------->"+responseEntity);
//        return success(true);
//
//    }



    @PostMapping("/entryphone")
    @Operation(summary = "获取呼叫中心用户信息通过手机号码并绑定")
    @PreAuthorize("@ss.hasPermission('crm:callcenter:user')")
    public CommonResult<Boolean> getCallCenterUserByPhone(@RequestBody String phone)  {
        Map<String,Object> param = new HashMap<>();
        param.put("phone",phone);
        HttpEntity<String> requestEntity = new HttpEntity<>(JSONUtil.toJsonStr(param),crmCallCenterService.getHeaders(null));
        ResponseEntity<String> responseEntity = restTemplate.exchange("https://phone.yunkecn.com/open/user/getUserByPhone", HttpMethod.POST, requestEntity, String.class);
        System.out.println("responseEntity------->"+responseEntity);

        Map<String,Object> param2 = new HashMap<>();
        param2.put("phone",phone);
        param2.put("userId","134");
//        CrmCallcenterUserDO crmCallcenterUserDO = crmCallCenterService.getCallCenterUser(phone);
//        param2.put("userId",crmCallcenterUserDO.getYunkeCallcenterUserId());

        HttpEntity<String> requestEntity2 = new HttpEntity<>(JSONUtil.toJsonStr(param2),crmCallCenterService.getHeaders(null));
        ResponseEntity<String> responseEntity2 = restTemplate.exchange("https://phone.yunkecn.com/open/user/phonePass", HttpMethod.POST, requestEntity2, String.class);
        System.out.println("responseEntity------->"+responseEntity2);

        return success(true);
    }

    @PostMapping("/binding")
    @Operation(summary = "绑定呼叫中心用户")
    @PreAuthorize("@ss.hasPermission('crm:callcenter:binding')")
    public CommonResult<CrmCallcenterUserRespVO> bindingCallCenterUser(@Valid @RequestBody CrmCallcenterUserSaveReqVO userReqVO)  {
        return success(crmCallCenterService.bindingUser(userReqVO.getUserId(),userReqVO.getYunkeCallcenterUserId()));
    }

    @PostMapping("/call")
    @Operation(summary = "打电话外呼")
    @PreAuthorize("@ss.hasPermission('crm:callcenter:call')")
    public CommonResult<ResponseEntity<String>> call(@Valid  @RequestBody CrmCallcenterCallReqVO callReqVO){
        System.out.println("callReqVO------->"+callReqVO);
        return crmCallCenterService.call(callReqVO,getLoginUserId());
    }
}

class MD5 {

    private String inStr;

    private String outStr;

    private MessageDigest md5;

    /**
     * Constructs the MD5 object and sets the string whose MD5 is to be computed.
     *
     * @param inStr the <code>String</code> whose MD5 is to be computed
     */
    public MD5(String inStr) {
        this.inStr = inStr;
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Computes the MD5 fingerprint of a string.
     *
     * @return the MD5 digest of the input <code>String</code>
     */
    public String compute() {
        char[] charArray = this.inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];
        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];
        byte[] md5Bytes = this.md5.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        outStr = hexValue.toString();
        return outStr;
    }

    public String getSixteenBitsString() {
        this.compute();
        return outStr.substring(8, 24);
    }

    public static String stringToMD5(String plainText) {
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        int length = 32 - md5code.length();
        for (int i = 0; i < length; i++) {
            md5code = "0" + md5code;
        }
        return md5code;
    }
}
