package com.florian.sun.spring.template.application.user.assembler;

import com.florian.sun.spring.template.application.user.dto.req.ChangePasswordRequestDTO;
import com.florian.sun.spring.template.application.user.dto.req.PageUsersRequestDTO;
import com.florian.sun.spring.template.application.user.dto.req.UpdateProfileRequestDTO;
import com.florian.sun.spring.template.application.user.dto.res.UserItemResponseDTO;
import com.florian.sun.spring.template.application.user.dto.res.UserProfileResponseDTO;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.param.ChangePasswordParam;
import com.florian.sun.spring.template.domain.user.model.param.DisableUserParam;
import com.florian.sun.spring.template.domain.user.model.param.PageUserQuery;
import com.florian.sun.spring.template.domain.user.model.param.UpdateProfileParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 用户场景 DTO <-> 领域对象转换
 * 根实体属性通过 target = "." 平铺到 ResponseDTO
 *
 * @author Florian Sun
 */
@Mapper(componentModel = "spring")
public interface UserAssembler {

    @Mapping(target = "userId", source = "operatorId")
    UpdateProfileParam toUpdateProfileParam(UpdateProfileRequestDTO dto);

    @Mapping(target = "userId", source = "operatorId")
    @Mapping(target = "oldRawPassword", source = "oldPassword")
    @Mapping(target = "newRawPassword", source = "newPassword")
    ChangePasswordParam toChangePasswordParam(ChangePasswordRequestDTO dto);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "operatorId", source = "operatorId")
    DisableUserParam toDisableUserParam(Long userId, Long operatorId);

    PageUserQuery toPageUserQuery(PageUsersRequestDTO dto);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = ".", source = "user")
    UserProfileResponseDTO toUserProfileResponseDTO(UserAggregate aggregate);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = ".", source = "user")
    UserItemResponseDTO toUserItemResponseDTO(UserAggregate aggregate);
}
