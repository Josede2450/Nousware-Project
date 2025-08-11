package com.nousware.service;

import com.nousware.entities.Faq;
import com.nousware.repository.FaqRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FaqServiceImpl implements FaqService {

    private final FaqRepository repo;

    public FaqServiceImpl(FaqRepository repo) { this.repo = repo; }

    @Transactional
    @Override
    public Faq create(Faq input) {
        String q = sanitize(input.getQuestion());
        String a = sanitize(input.getAnswer());
        if (repo.existsByQuestionIgnoreCase(q)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question already exists");
        }
        Faq faq = new Faq();
        faq.setQuestion(q);
        faq.setAnswer(a);
        return repo.save(faq);
    }

    @Transactional
    @Override
    public Faq update(Integer id, Faq input) {
        Faq existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ not found"));
        String q = sanitize(input.getQuestion());
        String a = sanitize(input.getAnswer());
        if (!existing.getQuestion().equalsIgnoreCase(q) && repo.existsByQuestionIgnoreCase(q)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question already exists");
        }
        existing.setQuestion(q);
        existing.setAnswer(a);
        return repo.save(existing);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ not found");
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Faq get(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Faq> list(String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            String kw = q.trim();
            return repo.findByQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCase(kw, kw, pageable);
        }
        return repo.findAll(pageable);
    }

    private String sanitize(String s) {
        if (s == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question/answer required");
        return s.trim();
    }
}
