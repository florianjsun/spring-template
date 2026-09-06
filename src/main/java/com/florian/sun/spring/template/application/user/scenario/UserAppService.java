package com.florian.sun.spring.template.application.user.scenario;

import com.florian.sun.spring.template.application.auth.adaptor.SessionAdaptor;
import com.florian.sun.spring.template.application.user.assembler.UserAssembler;
import com.florian.sun.spring.template.application.user.dto.req.ChangePasswordRequestDTO;
import com.florian.sun.spring.template.application.user.dto.req.UpdateProfileRequestDTO;
import com.florian.sun.spring.template.common.exception.BizAssert;
import com.florian.sun.spring.template.domain.file.model.enums.FileErrorCode;
import com.florian.sun.spring.template.domain.file.repository.FileRepository;
import com.florian.sun.spring.template.domain.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户写场景：修改资料 / 修改密码 / 禁用 / 启用
 *
 * @author Florian Sun
 */
@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserDomainService userDomainService;
    private final FileRepository fileRepository;
    private final SessionAdaptor sessionAdaptor;
    private final UserAssembler userAssembler;

    /**
     * 修改资料；头像引用文件聚合，跨聚合存在性校验放在应用层
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileRequestDTO dto) {
        if (dto.getAvatarFileId() != null) {
            BizAssert.isTrue(fileRepository.existsById(dto.getAvatarFileId()), FileErrorCode.FILE_NOT_FOUND);
        }
        userDomainService.updateProfile(userAssembler.toUpdateProfileParam(dto));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequestDTO dto) {
        userDomainService.changePassword(userAssembler.toChangePasswordParam(dto));
    }

    /**
     * 禁用用户并踢下线
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId, Long operatorId) {
        userDomainService.disableUser(userAssembler.toDisableUserParam(userId, operatorId));
        sessionAdaptor.kickout(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        userDomainService.enableUser(userId);
    }
}
