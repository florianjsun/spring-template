package com.florian.sun.spring.template.config;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 配置
 * 用 markerInterface 限定只扫描继承 BaseMapper 的接口，避免把 MapStruct 的 @Mapper 接口误注册为 MyBatis Mapper
 *
 * @author Florian Sun
 */
@Configuration
@MapperScan(
        basePackages = "com.florian.sun.spring.template.infrastructure",
        markerInterface = BaseMapper.class
)
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 逻辑删除：正常 0，已删除 1（与 BasePO.deleted 字段配合）
        globalConfig.setNormalValueOfLogicDelete(0);
        globalConfig.setDeletedValueOfLogicDelete(1);
    }
}
