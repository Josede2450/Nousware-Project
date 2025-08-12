package com.nousware.repository;

import com.nousware.entities.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    Page<Address> findAllByUser_UserId(Integer userId, Pageable pageable);

    Optional<Address> findByAddressIdAndUser_UserId(Integer addressId, Integer userId);

    boolean existsByAddressIdAndUser_UserId(Integer addressId, Integer userId);
}
