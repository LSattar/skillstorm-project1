package com.shelfsync.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.CategoryDto;
import com.shelfsync.exceptions.ResourceConflictException;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Category;
import com.shelfsync.repositories.CategoryRepository;
import com.shelfsync.repositories.ItemRepository;

/**
 * Service for managing item category operations.
 * 
 * <p>Handles CRUD operations for categories, which are used to classify inventory items.
 * Categories must have unique names and can be associated with multiple items.
 * 
 * <p>Key business rules:
 * <ul>
 *   <li>Category names must be unique</li>
 *   <li>Categories cannot be deleted if they are associated with existing items</li>
 * </ul>
 */
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

	/**
	 * Creates a new category with the specified name.
	 * 
	 * <p>Validates that the category name is unique before creating the category.
	 * 
	 * @param dto The category data transfer object containing the category name
	 * @return The created category
	 * @throws ResourceConflictException if the category name is already in use
	 */
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

	/**
	 * Retrieves all categories in the system.
	 * 
	 * @return A list of all categories
	 */
	public List<CategoryDto> findAllCategories() {
        log.debug("Fetching all Categories");
        List<Category> categories = repo.findAll();
        log.info("Fetched {} Categories", categories.size());
        return categories.stream().map(this::toDto).toList();
	}

	/**
	 * Retrieves a specific category by its ID.
	 * 
	 * @param id The category ID
	 * @return The category
	 * @throws ResourceNotFoundException if the category does not exist
	 */
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

	/**
	 * Updates an existing category's name.
	 * 
	 * @param id The category ID
	 * @param dto The category data transfer object with updated name
	 * @return The updated category
	 * @throws ResourceNotFoundException if the category does not exist
	 */
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

	/**
	 * Deletes a category by its ID.
	 * 
	 * <p>Validates that the category is not in use before deletion. A category
	 * cannot be deleted if it is associated with any existing items.
	 * 
	 * @param id The category ID to delete
	 * @throws ResourceNotFoundException if the category does not exist
	 * @throws ResourceConflictException if the category is associated with existing items
	 */
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
