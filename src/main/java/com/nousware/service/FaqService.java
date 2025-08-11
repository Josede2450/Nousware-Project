package com.nousware.service;

import com.nousware.entities.Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FaqService {
    Faq create(Faq faq);
    Faq update(Integer id, Faq faq);
    void delete(Integer id);
    Faq get(Integer id);
    Page<Faq> list(String q, Pageable pageable);
}
