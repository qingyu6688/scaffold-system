package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.mapper.SystemManagementMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserPostPolicyTest {
    @Test
    void retainsExistingInactivePostButRejectsNewInactiveAssignment() {
        var mapper = mock(SystemManagementMapper.class);
        when(mapper.selectUserPostIds(8L)).thenReturn(List.of(2L));
        var policy = new UserAccessPolicy(null, mapper, null);
        policy.checkPosts(8L, List.of(2L));
        assertThatThrownBy(() -> policy.checkPosts(8L, List.of(2L, 3L)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("岗位不存在或已停用");
        verify(mapper, never()).countActivePost(2L);
    }
}
