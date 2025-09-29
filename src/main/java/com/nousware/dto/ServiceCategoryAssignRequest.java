package com.nousware.dto;

import java.util.List;

// For assigning/replacing categories on a service
public record ServiceCategoryAssignRequest(
        List<Integer> categoryIds
) {}
