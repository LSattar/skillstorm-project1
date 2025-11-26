package com.shelfsync.services;

import java.util.List;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import com.shelfsync.dtos.CategoryDto;
import com.shelfsync.exceptions.*;
import com.shelfsync.models.Category;
import com.shelfsync.repositories.CategoryRepository;
import com.shelfsync.repositories.ItemRepository;

@Service
public class CategoryService {
	
    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

	private final CategoryRepository repo;
    private final ItemRepository itemRepo;

	public CategoryService(CategoryRepository repo, ItemRepository itemRepo) {
		this.repo = repo;
		this.itemRepo = itemRepo;
	}

	private CategoryDto toDto(Category category) {
		return new CategoryDto(category.getCategoryId(), category.getCategoryName());
	}

	// CREATE
	public CategoryDto create(CategoryDto dto) {
		log.debug("Request to create Category with name='{}'", dto.categoryName());
		
		if (repo.existsByCategoryName(dto.categoryName())) {
			log.warn("Conflict creating Category: name='{}' already exists", dto.categoryName());
			throw new ResourceConflictException("Category name must be unique");
		}
		Category category = new Category();
		category.setCategoryName(dto.categoryName());

		Category saved = repo.save(category);
		log.info("Created Category id={} name='{}'", saved.getCategoryId(), saved.getCategoryName());
		return toDto(saved);
	}

	// READ ALL
	public List<CategoryDto> findAllCategories() {
        log.debug("Fetching all Categories");
        List<Category> categories = repo.findAll();
        log.info("Fetched {} Categories", categories.size());
        return categories.stream().map(this::toDto).toList();
	}

	// READ ONE
	public CategoryDto findById(Integer id) {
        log.debug("Fetching Category by id={}", id);
        Category category = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found for id={}", id);
                    return new ResourceNotFoundException("Category not found: " + id);
                });

        log.info("Found Category id={} name='{}'", category.getCategoryId(), category.getCategoryName());
        return toDto(category);
	}

	// UPDATE
	public CategoryDto update(Integer id, CategoryDto dto) {
        log.debug("Updating Category id={} with name='{}'", id, dto.categoryName());

        Category existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update: Category not found for id={}", id);
                    return new ResourceNotFoundException("Category not found: " + id);
                });

        existing.setCategoryName(dto.categoryName());
        Category saved = repo.save(existing);

        log.info("Updated Category id={} to name='{}'", saved.getCategoryId(), saved.getCategoryName());
        return toDto(saved);
	}

	// DELETE
	public void deleteById(Integer id) {
        log.debug("Deleting Category id={}", id);

        Category existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete: Category not found for id={}", id);
                    return new ResourceNotFoundException("Category not found: " + id);
                });
        
        if (itemRepo.existsByCategory_CategoryId(id)) {
            log.warn("Cannot delete Category id={} because it is referenced by existing items", id);
            throw new ResourceConflictException("Category is in use by existing items and cannot be deleted");
        }

        repo.delete(existing);
        log.info("Deleted Category id={}", id);
	}
}
