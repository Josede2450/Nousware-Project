package com.nousware.service;

import com.nousware.entities.ContactForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactFormService {
    ContactForm create(ContactForm input);            // public (sends email)
    ContactForm get(Integer id);                      // admin
    Page<ContactForm> list(String q, Pageable p);     // admin
    void delete(Integer id);                          // admin
}
