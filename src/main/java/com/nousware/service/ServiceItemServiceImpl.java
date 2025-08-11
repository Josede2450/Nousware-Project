package com.nousware.service;

import com.nousware.entities.Service;
import com.nousware.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.stereotype.Service // avoid clash with your entity name
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceRepository repo;

    public ServiceItemServiceImpl(ServiceRepository repo) {
        this.repo = repo;
    }

    @Transactional
    @Override
    public Service create(Service input) {
        String title = sanitize(input.getTitle());
        String desc  = sanitize(input.getDescription());

        if (repo.existsByTitleIgnoreCase(title)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Title already exists");
        }

        Service s = new Service();
        s.setTitle(title);
        s.setDescription(desc);
        return repo.save(s);
    }

    @Transactional
    @Override
    public Service update(Integer id, Service input) {
        Service existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        String title = sanitize(input.getTitle());
        String desc  = sanitize(input.getDescription());

        if (!existing.getTitle().equalsIgnoreCase(title) && repo.existsByTitleIgnoreCase(title)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Title already exists");
        }

        existing.setTitle(title);
        existing.setDescription(desc);
        return repo.save(existing);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Service get(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Service> list(String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            String kw = q.trim();
            return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(kw, kw, pageable);
        }
        return repo.findAll(pageable);
    }

    private String sanitize(String s) {
        if (s == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title/description required");
        return s.trim();
    }
}
