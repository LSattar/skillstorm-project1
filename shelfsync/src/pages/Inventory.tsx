import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Form, InputGroup, Button, Dropdown } from 'react-bootstrap';
import { InventoryTable } from '../components/InventoryTable';
import { InventoryTransactionModal } from '../components/InventoryTransactionModal';
import { ItemModal } from '../components/ItemModal';

interface ItemWarehouseQuantity {
    warehouseId: number;
    warehouseName: string;
    quantity: number;
}

interface InventoryItem {
    itemId: number;
    sku: string;
    gameTitle: string;
    category: string | null;
    totalQuantity: number;
    locations: ItemWarehouseQuantity[];
}

export const Inventory = () => {
    const [searchQuery, setSearchQuery] = useState<string>('');
    const [inventoryItems, setInventoryItems] = useState<InventoryItem[]>([]);
    const [allItems, setAllItems] = useState<InventoryItem[]>([]);
    const [searchResults, setSearchResults] = useState<InventoryItem[] | null>(null); // null means no search, array means search results
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [showTransactionModal, setShowTransactionModal] = useState(false);
    const [showItemModal, setShowItemModal] = useState(false);
    const [editingItemId, setEditingItemId] = useState<number | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);
    
    const [selectedWarehouses, setSelectedWarehouses] = useState<Set<number>>(new Set());
    const [selectedCategories, setSelectedCategories] = useState<Set<string>>(new Set());
    const [warehouses, setWarehouses] = useState<Array<{id: number; name: string}>>([]);
    const [categories, setCategories] = useState<string[]>([]);

    const loadAllInventory = async () => {
        try {
            setLoading(true);
            setError(null);
            
            const [itemsResponse, warehouseItemsResponse] = await Promise.all([
                fetch('http://localhost:8080/item'),
                fetch('http://localhost:8080/warehouse-item')
            ]);
            
            if (!itemsResponse.ok || !warehouseItemsResponse.ok) {
                throw new Error(`HTTP error! status: ${itemsResponse.status}`);
            }

            const allItemsData = await itemsResponse.json();
            const warehouseItemsData = await warehouseItemsResponse.json();
            
            const itemMap = new Map<number, {
                item: any;
                locations: ItemWarehouseQuantity[];
                totalQuantity: number;
            }>();

            allItemsData.forEach((item: any) => {
                itemMap.set(item.id, {
                    item: item,
                    locations: [],
                    totalQuantity: 0
                });
            });

            warehouseItemsData.forEach((wi: any) => {
                const itemId = wi.item.itemId;
                if (!itemMap.has(itemId)) {
                    itemMap.set(itemId, {
                        item: wi.item,
                        locations: [],
                        totalQuantity: 0
                    });
                }
                
                const entry = itemMap.get(itemId)!;
                entry.locations.push({
                    warehouseId: wi.warehouseId,
                    warehouseName: wi.warehouseName,
                    quantity: wi.quantity || 0
                });
                entry.totalQuantity += wi.quantity || 0;
            });

            const groupedItems: InventoryItem[] = Array.from(itemMap.values()).map(entry => ({
                itemId: entry.item.id || entry.item.itemId,
                sku: entry.item.sku,
                gameTitle: entry.item.gameTitle,
                category: entry.item.category?.categoryName || null,
                totalQuantity: entry.totalQuantity,
                locations: entry.locations
            }));

            const uniqueCategories = Array.from(new Set(groupedItems.map(item => item.category).filter(Boolean))) as string[];
            setCategories(uniqueCategories.sort());

            setAllItems(groupedItems);
            setInventoryItems(groupedItems);
        } catch (err: any) {
            setError('Failed to load inventory: ' + (err.message || 'Unknown error'));
            console.error('Error loading inventory:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        const fetchWarehouses = async () => {
            try {
                const response = await fetch('http://localhost:8080/warehouse');
                if (response.ok) {
                    const data = await response.json();
                    setWarehouses(data);
                }
            } catch (err) {
                console.error('Error fetching warehouses:', err);
            }
        };
        fetchWarehouses();
    }, []);

    useEffect(() => {
        loadAllInventory();
    }, []);

    useEffect(() => {
        if (refreshKey > 0) {
            loadAllInventory();
        }
    }, [refreshKey]);

    // Debounced auto-search effect
    useEffect(() => {
        const query = searchQuery.trim();
        
        if (!query) {
            // Clear search results when query is empty
            setSearchResults(null);
            setError(null);
            return;
        }

        // Debounce the search
        const timeoutId = setTimeout(async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await fetch(`http://localhost:8080/inventory/search?q=${encodeURIComponent(query)}`);
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                const mappedData: InventoryItem[] = data.map((item: any) => {
                    const fullItem = allItems.find(ai => ai.itemId === item.itemId);
                    return {
                        itemId: item.itemId,
                        sku: item.sku,
                        gameTitle: item.gameTitle,
                        category: fullItem?.category || null,
                        totalQuantity: item.totalQuantity,
                        locations: item.locations || []
                    };
                });
                
                setSearchResults(mappedData);
            } catch (err: any) {
                setError('Failed to search inventory: ' + (err.message || 'Unknown error'));
                console.error('Error searching inventory:', err);
                setSearchResults([]);
            } finally {
                setLoading(false);
            }
        }, 500); // 500ms delay

        return () => clearTimeout(timeoutId);
    }, [searchQuery, allItems]);

    // Apply filters to either search results or all items
    useEffect(() => {
        const baseItems = searchResults !== null ? searchResults : allItems;
        let filtered = [...baseItems];

        if (selectedWarehouses.size > 0) {
            filtered = filtered.filter(item => 
                item.locations.some(loc => selectedWarehouses.has(loc.warehouseId))
            );
        }

        if (selectedCategories.size > 0) {
            filtered = filtered.filter(item => 
                item.category && selectedCategories.has(item.category)
            );
        }

        setInventoryItems(filtered);
    }, [selectedWarehouses, selectedCategories, allItems, searchResults]);

    const handleSearch = async (e?: React.FormEvent) => {
        if (e) {
            e.preventDefault();
        }

        const query = searchQuery.trim();
        
        if (!query) {
            setSearchResults(null);
            setError(null);
            return;
        }

        try {
            setLoading(true);
            setError(null);
            const response = await fetch(`http://localhost:8080/inventory/search?q=${encodeURIComponent(query)}`);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            const mappedData: InventoryItem[] = data.map((item: any) => {
                const fullItem = allItems.find(ai => ai.itemId === item.itemId);
                return {
                    itemId: item.itemId,
                    sku: item.sku,
                    gameTitle: item.gameTitle,
                    category: fullItem?.category || null,
                    totalQuantity: item.totalQuantity,
                    locations: item.locations || []
                };
            });
            
            setSearchResults(mappedData);
        } catch (err: any) {
            setError('Failed to search inventory: ' + (err.message || 'Unknown error'));
            console.error('Error searching inventory:', err);
            setSearchResults([]);
        } finally {
            setLoading(false);
        }
    };

    const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            handleSearch();
        }
    };

    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const query = e.target.value;
        setSearchQuery(query);
    };

    const handleClearSearch = () => {
        setSearchQuery('');
        setSearchResults(null);
        setError(null);
    };

    const handleWarehouseFilterChange = (warehouseId: number, checked: boolean) => {
        setSelectedWarehouses(prev => {
            const newSet = new Set(prev);
            if (checked) {
                newSet.add(warehouseId);
            } else {
                newSet.delete(warehouseId);
            }
            return newSet;
        });
    };

    const handleCategoryFilterChange = (category: string, checked: boolean) => {
        setSelectedCategories(prev => {
            const newSet = new Set(prev);
            if (checked) {
                newSet.add(category);
            } else {
                newSet.delete(category);
            }
            return newSet;
        });
    };

    const handleSaveTransaction = async (transaction: {
        itemId: number;
        fromWarehouseId: number | null;
        toWarehouseId: number | null;
        quantityChange: number;
        transactionType: string;
        reason: string | null;
        performedByEmployeeId: string | null;
    }) => {
        const response = await fetch('http://localhost:8080/inventory-history', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                id: null,
                itemId: transaction.itemId,
                fromWarehouseId: transaction.fromWarehouseId,
                toWarehouseId: transaction.toWarehouseId,
                quantityChange: transaction.quantityChange,
                transactionType: transaction.transactionType,
                reason: transaction.reason,
                occurredAt: null, 
                performedByEmployeeId: transaction.performedByEmployeeId
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ detail: 'Failed to create transaction' }));
            // ProblemDetail uses "detail" field, but also check "message" for compatibility
            throw new Error(errorData.detail || errorData.message || 'Failed to create transaction');
        }

        setRefreshKey(prev => prev + 1);
    };

    const handleSaveItem = async (item: {
        sku: string;
        gameTitle: string;
        categoryId: number | null;
        companyId: number | null;
        weightLbs: number;
        cubicFeet: number;
    }) => {
        const isEditing = editingItemId !== null;
        const url = isEditing 
            ? `http://localhost:8080/item/${editingItemId}`
            : 'http://localhost:8080/item';
        
        const response = await fetch(url, {
            method: isEditing ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                id: isEditing ? editingItemId : null,
                sku: item.sku,
                gameTitle: item.gameTitle,
                categoryId: item.categoryId,
                companyId: item.companyId,
                weightLbs: item.weightLbs,
                cubicFeet: item.cubicFeet
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Failed to ${isEditing ? 'update' : 'create'} item` }));
            throw new Error(errorData.message || `Failed to ${isEditing ? 'update' : 'create'} item`);
        }

        setEditingItemId(null);
        setRefreshKey(prev => prev + 1);
    };

    const handleEdit = (itemId: number) => {
        setEditingItemId(itemId);
        setShowItemModal(true);
    };

    const handleDelete = async (itemId: number, itemName: string) => {
        if (!window.confirm(`Are you sure you want to delete "${itemName}"? This action cannot be undone.`)) {
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/item/${itemId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Failed to delete item' }));
                throw new Error(errorData.message || 'Failed to delete item');
            }

            setRefreshKey(prev => prev + 1);
        } catch (err: any) {
            alert('Error deleting item: ' + (err.message || 'Unknown error'));
            console.error('Error deleting item:', err);
        }
    };

    const handleCloseItemModal = () => {
        setShowItemModal(false);
        setEditingItemId(null);
    };

    const handleAddNew = () => {
        setEditingItemId(null);
        setShowItemModal(true);
    };

    return (
        <Container className="mt-3">
            <Row>
                <Col><h1>Inventory</h1></Col>
                <Col className="text-end">
                    <Button variant="outline-primary" onClick={handleAddNew} className="me-2">
                        <i className="bi bi-plus-circle me-1"></i>
                        Add Item
                    </Button>
                    <Button onClick={() => setShowTransactionModal(true)}>
                        <i className="bi bi-plus-circle me-1"></i>
                        Record Transaction
                    </Button>
                </Col>
            </Row>
            <Row className="mb-3">
                <Col>
                    <div className="d-flex align-items-center gap-3">
                        <Form onSubmit={handleSearch} className="flex-grow-1">
                            <InputGroup>
                                <Form.Control
                                    type="text"
                                    placeholder="Search by SKU or Game Title..."
                                    value={searchQuery}
                                    onChange={handleSearchChange}
                                    onKeyPress={handleKeyPress}
                                />
                                {searchQuery && (
                                    <Button 
                                        variant="outline-secondary" 
                                        onClick={handleClearSearch}
                                        title="Clear search"
                                    >
                                        <i className="bi bi-x"></i>
                                    </Button>
                                )}
                                <Button variant="primary" type="submit" disabled={loading}>
                                    <i className="bi bi-search me-1"></i>
                                    {loading ? 'Searching...' : 'Search'}
                                </Button>
                            </InputGroup>
                        </Form>
                        <Dropdown>
                            <Dropdown.Toggle variant="outline-secondary" id="warehouse-filter-dropdown">
                                <i className="bi bi-building me-1"></i>
                                Warehouse {selectedWarehouses.size > 0 && `(${selectedWarehouses.size})`}
                            </Dropdown.Toggle>
                            <Dropdown.Menu style={{ maxHeight: '300px', overflowY: 'auto', minWidth: '200px' }}>
                                {warehouses.map((warehouse) => (
                                    <Dropdown.Item key={warehouse.id} as="div">
                                        <Form.Check
                                            type="checkbox"
                                            id={`warehouse-${warehouse.id}`}
                                            label={warehouse.name}
                                            checked={selectedWarehouses.has(warehouse.id)}
                                            onChange={(e) => {
                                                e.stopPropagation();
                                                handleWarehouseFilterChange(warehouse.id, e.target.checked);
                                            }}
                                            onClick={(e) => e.stopPropagation()}
                                        />
                                    </Dropdown.Item>
                                ))}
                            </Dropdown.Menu>
                        </Dropdown>
                        <Dropdown>
                            <Dropdown.Toggle variant="outline-secondary" id="category-filter-dropdown">
                                <i className="bi bi-tags me-1"></i>
                                Category {selectedCategories.size > 0 && `(${selectedCategories.size})`}
                            </Dropdown.Toggle>
                            <Dropdown.Menu style={{ maxHeight: '300px', overflowY: 'auto', minWidth: '200px' }}>
                                {categories.map((category) => (
                                    <Dropdown.Item key={category} as="div">
                                        <Form.Check
                                            type="checkbox"
                                            id={`category-${category}`}
                                            label={category}
                                            checked={selectedCategories.has(category)}
                                            onChange={(e) => {
                                                e.stopPropagation();
                                                handleCategoryFilterChange(category, e.target.checked);
                                            }}
                                            onClick={(e) => e.stopPropagation()}
                                        />
                                    </Dropdown.Item>
                                ))}
                            </Dropdown.Menu>
                        </Dropdown>
                    </div>
                </Col>
            </Row>
            <Row>
                <Col>
                    <InventoryTable 
                        items={inventoryItems} 
                        loading={loading} 
                        error={error}
                        onEdit={handleEdit}
                        onDelete={handleDelete}
                    />
                </Col>
            </Row>
            <InventoryTransactionModal
                show={showTransactionModal}
                onHide={() => setShowTransactionModal(false)}
                onSave={handleSaveTransaction}
            />
            <ItemModal
                show={showItemModal}
                onHide={handleCloseItemModal}
                itemId={editingItemId}
                onSave={handleSaveItem}
            />
        </Container>
    );
}
