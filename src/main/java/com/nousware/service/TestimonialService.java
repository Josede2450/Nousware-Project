package com.nousware.service;

import com.nousware.entities.Testimonial;
import com.nousware.dto.ViewTestimonial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TestimonialService {

    Testimonial create(Testimonial input);

    Testimonial update(Integer id, Testimonial input);

    void delete(Integer id);

    Testimonial get(Integer id);

    // 🔍 Basic list with search and favorite filter
    Page<Testimonial> list(String q, Boolean favorite, Pageable pageable);

    // ✅ Optional: for frontend (DTO view)
    Page<ViewTestimonial> listView(String q, Boolean favorite, Pageable pageable);

    ViewTestimonial getView(Integer id);
}
