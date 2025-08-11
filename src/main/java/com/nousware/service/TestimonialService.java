package com.nousware.service;

import com.nousware.entities.Testimonial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TestimonialService {
    Testimonial create(Testimonial input);
    Testimonial update(Integer id, Testimonial input);
    void delete(Integer id);
    Testimonial get(Integer id);
    Page<Testimonial> list(String q, Integer userId, Pageable pageable);
}
