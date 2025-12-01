package com.shelfsync.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.CompanyDto;
import com.shelfsync.exceptions.ResourceConflictException;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Company;
import com.shelfsync.repositories.CompanyRepository;
import com.shelfsync.repositories.ItemRepository;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository repo;
    private final ItemRepository itemRepo;

    public CompanyService(CompanyRepository repo, ItemRepository itemRepo) {
        this.repo = repo;
        this.itemRepo = itemRepo;
    }

    private CompanyDto toDto(Company company) {
        return new CompanyDto(
                company.getCompanyId(),
                company.getName(),
                company.getPhone(),
                company.getEmail(),
                company.getContactPerson()
        );
    }

    // CREATE
    public CompanyDto create(CompanyDto dto) {
        log.debug("Request to create Company with name='{}'", dto.name());

        Company company = new Company();
        company.setName(dto.name());
        company.setPhone(dto.phone());
        company.setEmail(dto.email());
        company.setContactPerson(dto.contactPerson());

        Company saved = repo.save(company);
        log.info("Created Company id={} name='{}'", saved.getCompanyId(), saved.getName());
        return toDto(saved);
    }

    // READ ALL
    public List<CompanyDto> findAllCompanies() {
        log.debug("Fetching all Companies");
        List<Company> companies = repo.findAll();
        log.info("Fetched {} Companies", companies.size());
        return companies.stream().map(this::toDto).toList();
    }

    // READ ONE
    public CompanyDto findById(Integer id) {
        log.debug("Fetching Company by id={}", id);
        Company company = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Company not found for id={}", id);
                    return new ResourceNotFoundException("Company not found: " + id);
                });

        log.info("Found Company id={} name='{}'", company.getCompanyId(), company.getName());
        return toDto(company);
    }

    // UPDATE
    public CompanyDto update(Integer id, CompanyDto dto) {
        log.debug("Updating Company id={} with name='{}'", id, dto.name());

        Company existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update: Company not found for id={}", id);
                    return new ResourceNotFoundException("Company not found: " + id);
                });

        existing.setName(dto.name());
        existing.setPhone(dto.phone());
        existing.setEmail(dto.email());
        existing.setContactPerson(dto.contactPerson());

        Company saved = repo.save(existing);
        log.info("Updated Company id={} to name='{}'", saved.getCompanyId(), saved.getName());
        return toDto(saved);
    }

    // DELETE
    public void deleteById(Integer id) {
        log.debug("Deleting Company id={}", id);

        Company existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete: Company not found for id={}", id);
                    return new ResourceNotFoundException("Company not found: " + id);
                });

        if (itemRepo.existsByCompany_CompanyId(id)) {
            log.warn("Cannot delete Company id={} because it is referenced by existing items", id);
            throw new ResourceConflictException("Company is in use by existing items and cannot be deleted");
        }

        repo.delete(existing);
        log.info("Deleted Company id={}", id);
    }
}
