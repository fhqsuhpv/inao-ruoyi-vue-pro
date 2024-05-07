package cn.iocoder.yudao.module.crm.controller.admin.callcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 用户与呼叫中心用户绑定关系新增/修改 Request VO")
@Data
public class CrmCallcenterUserSaveReqVO {

    @Schema(description = "主建", requiredMode = Schema.RequiredMode.REQUIRED, example = "30755")
    private Long id;

    @Schema(description = "用户id", example = "2642")
    @NotEmpty(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "云客用户id", example = "23969")
    private String yunkeCallcenterUserId;

    @Schema(description = "云客手机号")
    @NotEmpty(message = "云客手机号不能为空")
    private String yunkeCallcenterPhone;

    @Schema(description = "连连用户id", example = "18461")
    private String lianlianCallcenterUserId;

    @Schema(description = "连连手机号")
    private String lianlianCallcenterPhone;

}