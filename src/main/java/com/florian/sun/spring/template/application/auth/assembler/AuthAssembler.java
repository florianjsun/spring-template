package com.florian.sun.spring.template.application.auth.assembler;

import com.florian.sun.spring.template.application.auth.adaptor.SessionDTO;
import com.florian.sun.spring.template.application.auth.dto.req.LoginRequestDTO;
import com.florian.sun.spring.template.application.auth.dto.req.RegisterRequestDTO;
import com.florian.sun.spring.template.application.auth.dto.res.LoginResponseDTO;
import com.florian.sun.spring.template.application.auth.dto.res.RegisterResponseDTO;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.param.AuthenticateParam;
import com.florian.sun.spring.template.domain.user.model.param.RegisterUserParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 认证场景 DTO <-> 领域对象转换
 * 根实体属性通过 target = "." 平铺到 ResponseDTO
 *
 * @author Florian Sun
 */
@Mapper(componentModel = "spring")
public interface AuthAssembler {

    @Mapping(target = "rawPassword", source = "password")
    RegisterUserParam toRegisterUserParam(RegisterRequestDTO dto);

    @Mapping(target = "rawPassword", source = "password")
    AuthenticateParam toAuthenticateParam(LoginRequestDTO dto);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = ".", source = "user")
    RegisterResponseDTO toRegisterResponseDTO(UserAggregate aggregate);

    @Mapping(target = "userId", source = "aggregate.id")
    @Mapping(target = ".", source = "aggregate.user")
    LoginResponseDTO toLoginResponseDTO(UserAggregate aggregate, SessionDTO session);
}
