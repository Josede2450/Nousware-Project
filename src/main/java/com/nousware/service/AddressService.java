package com.nousware.service;

import com.nousware.entities.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddressService {
    Address create(Address input);                       // owner or admin
    Address update(Integer id, Address input);           // owner or admin
    void delete(Integer id);                             // owner or admin
    Address get(Integer id);                             // owner or admin
    Page<Address> list(Pageable pageable, Integer userId); // admin: all or by userId; owner: own only (userId ignored)
}
