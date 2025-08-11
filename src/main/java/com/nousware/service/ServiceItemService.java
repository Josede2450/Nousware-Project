package com.nousware.service;

import com.nousware.entities.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceItemService {
    Service create(Service input);
    Service update(Integer id, Service input);
    void delete(Integer id);
    Service get(Integer id);
    Page<Service> list(String q, Pageable pageable);
}
