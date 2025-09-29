package com.nousware.dto;

import java.util.List;

public record ServiceDTO(
        Integer id,             // serviceId
        String title,
        String description,
        String summary,
        String imageUrl,
        boolean mostPopular,
        List<CategoryDTO> categories
) {}
