package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepartmentPathsTest {
    private final List<Map<String, Object>> tree = List.of(
            Map.of("id", 1L, "parentId", 0L), Map.of("id", 2L, "parentId", 1L),
            Map.of("id", 3L, "parentId", 2L), Map.of("id", 4L, "parentId", 3L),
            Map.of("id", 5L, "parentId", 1L));

    @Test
    void movesEveryDescendantAndLeavesOtherBranchesUntouched() {
        assertThat(DepartmentPaths.move(tree, 2L, 5L))
                .containsExactlyInAnyOrderEntriesOf(Map.of(2L, "1,5", 3L, "1,5,2", 4L, "1,5,2,3"));
    }

    @Test
    void rejectsSelfAndDescendantParents() {
        for (Long parent : List.of(2L, 3L, 4L)) {
            assertThatThrownBy(() -> DepartmentPaths.move(tree, 2L, parent))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("自身或下级");
        }
    }

    @Test
    void movesToRootAndRejectsMissingParent() {
        assertThat(DepartmentPaths.move(tree, 2L, 0L)).containsEntry(2L, "").containsEntry(4L, "2,3");
        assertThatThrownBy(() -> DepartmentPaths.move(tree, 2L, 999L)).isInstanceOf(BusinessException.class);
    }
}
