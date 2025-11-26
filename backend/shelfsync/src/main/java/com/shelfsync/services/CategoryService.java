package com.shelfsync.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shelfsync.dtos.CategoryDto;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Category;
import com.shelfsync.repositories.CategoryRepository;

@Service
public class CategoryService {
	
    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(category.getCategoryId(), category.getCategoryName());
    }

    // CREATE
    public CategoryDto create(CategoryDto dto) {
        Category category = new Category();
        category.setCategoryName(dto.categoryName());

        Category saved = repo.save(category);
        return toDto(saved);
    }
	
    // READ ALL
    public List<CategoryDto> findAllCategories() {
        return repo.findAll().stream()
                .map(this::toDto)
                .toList();
    }
	
    // READ ONE
    public CategoryDto findById(Integer id) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        return toDto(category);
    }
	
    // UPDATE
    public CategoryDto update(Integer id, CategoryDto dto) {
        Category existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        existing.setCategoryName(dto.categoryName());

        Category saved = repo.save(existing);
        return toDto(saved);
    }
	
    // DELETE
    public void deleteById(Integer id) {
        Category existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        repo.delete(existing);
    }
}
