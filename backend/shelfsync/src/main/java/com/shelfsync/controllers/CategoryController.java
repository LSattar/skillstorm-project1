package com.shelfsync.controllers;

import com.shelfsync.dtos.CategoryDto;
import com.shelfsync.services.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

	private final CategoryService service;

	public CategoryController(CategoryService service) {
		this.service = service;
	}

	// CREATE
	@PostMapping
	public ResponseEntity<CategoryDto> create(@RequestBody CategoryDto dto) {
		CategoryDto created = service.create(dto);
		return ResponseEntity.ok(created);
	}

	// READ ONE
	@GetMapping("/{id}")
	public ResponseEntity<CategoryDto> getById(@PathVariable Integer id) {
		CategoryDto dto = service.findById(id);
		return ResponseEntity.ok(dto);
	}

	// READ ALL
	@GetMapping
	public ResponseEntity<List<CategoryDto>> getAll() {
		List<CategoryDto> categories = service.findAllCategories();
		return ResponseEntity.ok(categories);
	}

	// UPDATE
	@PutMapping("/{id}")
	public ResponseEntity<CategoryDto> update(@PathVariable Integer id, @RequestBody CategoryDto dto) {
		CategoryDto updated = service.update(id, dto);
		return ResponseEntity.ok(updated);
	}

	// DELETE
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
