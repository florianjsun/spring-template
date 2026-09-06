package com.florian.sun.spring.template.application.user.scenario;

import com.florian.sun.spring.template.application.user.assembler.UserAssembler;
import com.florian.sun.spring.template.application.user.dto.req.PageUsersRequestDTO;
import com.florian.sun.spring.template.application.user.dto.res.UserItemResponseDTO;
import com.florian.sun.spring.template.application.user.dto.res.UserProfileResponseDTO;
import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.domain.user.model.aggregate.UserAggregate;
import com.florian.sun.spring.template.domain.user.model.enums.UserErrorCode;
import com.florian.sun.spring.template.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户查询场景（读模式）
 * 直接读仓储，不经过领域服务
 *
 * @author Florian Sun
 */
@Service
@RequiredArgsConstructor
public class UserQueryAppService {

    private final UserRepository userRepository;
    private final UserAssembler userAssembler;

    public UserProfileResponseDTO getMyProfile(Long operatorId) {
        return userAssembler.toUserProfileResponseDTO(loadUser(operatorId));
    }

    public UserProfileResponseDTO getUserDetail(Long userId) {
        return userAssembler.toUserProfileResponseDTO(loadUser(userId));
    }

    public PageResult<UserItemResponseDTO> pageUsers(PageUsersRequestDTO dto) {
        return userRepository.pageUsers(userAssembler.toPageUserQuery(dto))
                .map(userAssembler::toUserItemResponseDTO);
    }

    private UserAggregate loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
    }
}
