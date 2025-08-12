package com.nousware.service;

import com.nousware.entities.Address;
import com.nousware.entities.User;
import com.nousware.repository.AddressRepository;
import com.nousware.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository repo;
    private final UserRepository userRepo;

    public AddressServiceImpl(AddressRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @Transactional
    @Override
    public Address create(Address input) {
        Integer me = currentUserId();
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        // Admin can assign any owner via input.user.userId; owner always assigns to self
        User owner;
        if (isAdmin() && input.getUser() != null && input.getUser().getUserId() > 0) {
            owner = userRepo.findById(input.getUser().getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found"));
        } else {
            owner = userRepo.findById(me)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }

        Address a = new Address();
        a.setAddressType(trim(input.getAddressType()));
        a.setStreet(trim(input.getStreet()));
        a.setCity(trim(input.getCity()));
        a.setState(trim(input.getState()));
        a.setZipCode(trim(input.getZipCode()));
        a.setCountry(trim(input.getCountry()));
        a.setUpdatedAt(LocalDateTime.now());
        a.setUser(owner);

        return repo.save(a);
    }

    @Transactional
    @Override
    public Address update(Integer id, Address input) {
        Integer me = currentUserId();
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        Address existing = isAdmin()
                ? repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"))
                : repo.findByAddressIdAndUser_UserId(id, me)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));

        existing.setAddressType(trim(input.getAddressType()));
        existing.setStreet(trim(input.getStreet()));
        existing.setCity(trim(input.getCity()));
        existing.setState(trim(input.getState()));
        existing.setZipCode(trim(input.getZipCode()));
        existing.setCountry(trim(input.getCountry()));
        existing.setUpdatedAt(LocalDateTime.now());

        // Admin may reassign owner; owners cannot change owner
        if (isAdmin() && input.getUser() != null && input.getUser().getUserId() > 0) {
            User owner = userRepo.findById(input.getUser().getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found"));
            existing.setUser(owner);
        }

        return repo.save(existing);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        Integer me = currentUserId();
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        boolean allowed = isAdmin() ? repo.existsById(id) : repo.existsByAddressIdAndUser_UserId(id, me);
        if (!allowed) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found");

        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Address get(Integer id) {
        Integer me = currentUserId();
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        return isAdmin()
                ? repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"))
                : repo.findByAddressIdAndUser_UserId(id, me)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Address> list(Pageable pageable, Integer userId) {
        Integer me = currentUserId();
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        if (isAdmin()) {
            if (userId != null && userId > 0) return repo.findAllByUser_UserId(userId, pageable);
            return repo.findAll(pageable);
        }
        return repo.findAllByUser_UserId(me, pageable); // owner only
    }

    // ---- helpers ----
    private String trim(String s) { return s == null ? null : s.trim(); }

    private Integer currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        String email = auth.getName().trim().toLowerCase();
        Optional<User> me = userRepo.findByEmailIgnoreCase(email);
        return me.map(User::getUserId).orElse(null);
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) return true;
        }
        return false;
    }
}
