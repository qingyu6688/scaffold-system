package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 从父子关系重算路径，不信任可能过期的 ancestors 字段。 */
final class DepartmentPaths {
    private static final int MAX_PATH_LENGTH = 500;

    private DepartmentPaths() { }

    static Map<Long, String> move(List<Map<String, Object>> tree, Long id, Long parentId) {
        Map<Long, Long> parents = new LinkedHashMap<>();
        tree.forEach(row -> parents.put(number(row.get("id")), number(row.get("parentId"))));
        if (!parents.containsKey(id)) throw new BusinessException("部门不存在或已删除");
        if (parentId != null && parentId != 0 && !parents.containsKey(parentId)) {
            throw new BusinessException("上级部门不存在或已删除");
        }
        if (parentId != null) parents.put(id, parentId);
        Map<Long, String> result = new LinkedHashMap<>();
        for (Long node : parents.keySet()) {
            List<Long> ancestors = ancestors(node, parents);
            if (node.equals(id) || ancestors.contains(id)) {
                String path = String.join(",", ancestors.stream().map(String::valueOf).toList());
                if (path.length() > MAX_PATH_LENGTH) throw new BusinessException("部门层级过深");
                result.put(node, path);
            }
        }
        return result;
    }

    private static List<Long> ancestors(Long node, Map<Long, Long> parents) {
        List<Long> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(node);
        Long parent = parents.get(node);
        while (parent != null && parent != 0) {
            if (!visited.add(parent)) throw new BusinessException("上级部门不能是自身或下级部门");
            if (!parents.containsKey(parent)) throw new BusinessException("部门树存在无效上级，请先修复");
            result.add(0, parent);
            parent = parents.get(parent);
        }
        return result;
    }

    private static Long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
