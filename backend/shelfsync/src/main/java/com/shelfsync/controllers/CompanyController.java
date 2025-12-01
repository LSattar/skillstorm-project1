package com.shelfsync.controllers;

import com.shelfsync.dtos.CompanyDto;
import com.shelfsync.services.CompanyService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<CompanyDto> create(@Valid @RequestBody CompanyDto dto) {
        CompanyDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<CompanyDto> getById(@PathVariable Integer id) {
        CompanyDto dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<CompanyDto>> getAll() {
        List<CompanyDto> companies = service.findAllCompanies();
        return ResponseEntity.ok(companies);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CompanyDto> update(@PathVariable Integer id,
                                             @RequestBody CompanyDto dto) {
        CompanyDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
