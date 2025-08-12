package com.nousware.service;

import com.nousware.entities.Project;
import com.nousware.entities.User;
import com.nousware.repository.ProjectRepository;
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
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository repo;
    private final UserRepository userRepo;

    public ProjectServiceImpl(ProjectRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    // ----------------- ADMIN ONLY -----------------

    @Transactional
    @Override
    public Project create(Project input) {
        requireAdmin();

        Integer ownerId = (input.getUser() != null) ? input.getUser().getUserId() : null;
        if (ownerId == null || ownerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user.userId is required");
        }

        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found"));

        Project p = new Project();
        p.setGoals(trim(input.getGoals()));
        p.setBudget(input.getBudget());
        p.setDeadline(input.getDeadline());
        p.setPreferences(trim(input.getPreferences()));
        p.setCompetitorReferences(trim(input.getCompetitorReferences()));
        p.setStatus(trim(input.getStatus()));
        p.setUser(owner);

        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());

        return repo.save(p);
    }

    @Transactional
    @Override
    public Project update(Integer id, Project input) {
        requireAdmin();

        Project existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        existing.setGoals(trim(input.getGoals()));
        existing.setBudget(input.getBudget());   // <-- fixed here
        existing.setDeadline(input.getDeadline());
        existing.setPreferences(trim(input.getPreferences()));
        existing.setCompetitorReferences(trim(input.getCompetitorReferences()));
        existing.setStatus(trim(input.getStatus()));

        if (input.getUser() != null && input.getUser().getUserId() > 0) {
            User owner = userRepo.findById(input.getUser().getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found"));
            existing.setUser(owner);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        requireAdmin();
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        repo.deleteById(id);
    }

    // ----------------- OWNER OR ADMIN -----------------

    @Transactional(readOnly = true)
    @Override
    public Project get(Integer id) {
        Project p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (!isAdmin() && !isOwner(p.getUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to view this project");
        }
        return p;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Project> list(Pageable pageable) {
        if (isAdmin()) {
            return repo.findAll(pageable);
        }
        Integer me = currentUserId();
        if (me == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return repo.findAllByUser_UserId(me, pageable);
    }

    // ----------------- Helpers -----------------

    private String trim(String s) { return (s == null) ? null : s.trim(); }

    private void requireAdmin() {
        if (!isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) return true;
        }
        return false;
    }

    private boolean isOwner(User owner) {
        if (owner == null) return false;
        Integer me = currentUserId();
        return me != null && me.equals(owner.getUserId());
    }

    private Integer currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        String email = auth.getName().trim().toLowerCase();
        Optional<User> me = userRepo.findByEmailIgnoreCase(email);
        return me.map(User::getUserId).orElse(null);
    }
}
