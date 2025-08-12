package com.nousware.controller;

import com.nousware.entities.Address;
import com.nousware.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Address body) {
        Address created = service.create(body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Address created successfully");
        resp.put("address", created);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Address> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    // Admin: all or ?userId=; Owner: own only (userId ignored)
    @GetMapping
    public ResponseEntity<Page<Address>> list(Pageable pageable,
                                              @RequestParam(required = false) Integer userId) {
        return ResponseEntity.ok(service.list(pageable, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id,
                                                      @Valid @RequestBody Address body) {
        Address updated = service.update(id, body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Address updated successfully");
        resp.put("address", updated);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
    }
}
