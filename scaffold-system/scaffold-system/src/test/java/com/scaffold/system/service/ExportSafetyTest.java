package com.scaffold.system.service;

import com.scaffold.system.mapper.SystemManagementMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExportSafetyTest {
    @Test
    void prefixesFormulaCellsWithTextMarker() {
        var mapper = mock(SystemManagementMapper.class);
        when(mapper.selectPosts(null, null, 0, 5000)).thenReturn(List.of(
                Map.of("postCode", "=1+1", "postName", "  @SUM(1)", "status", 1)));
        var service = new SystemManagementService(mapper, null, null, null);
        assertThat(service.exportPosts(null, null)).contains("'=1+1,'  @SUM(1)");
    }
}
