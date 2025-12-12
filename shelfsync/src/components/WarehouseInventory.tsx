import React, { useEffect, useState } from 'react';
import { Table, Spinner, Pagination } from 'react-bootstrap';

interface WarehouseInventoryProps {
    warehouseId: number;
}

interface WarehouseItem {
    warehouseId: number;
    warehouseName: string;
    item: {
        itemId: number;
        sku: string;
        gameTitle: string;
    };
    quantity: number;
}

const ITEMS_PER_PAGE = 10;

export const WarehouseInventory: React.FC<WarehouseInventoryProps> = ({ warehouseId }) => {
    const [inventory, setInventory] = useState<WarehouseItem[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [currentPage, setCurrentPage] = useState<number>(1);

    useEffect(() => {
        const fetchInventory = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await fetch('http://localhost:8080/warehouse-item');
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                // Filter items for this warehouse and exclude items with 0 quantity
                const warehouseItems = data.filter((wi: WarehouseItem) => 
                    wi.warehouseId === warehouseId && (wi.quantity || 0) > 0
                );
                setInventory(warehouseItems);
                setCurrentPage(1); // Reset to first page when data changes
            } catch (err: any) {
                setError('Failed to load inventory: ' + (err.message || 'Unknown error'));
                console.error('Error fetching warehouse inventory:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchInventory();
    }, [warehouseId]);

    // Calculate pagination
    const totalPages = Math.ceil(inventory.length / ITEMS_PER_PAGE);
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const endIndex = startIndex + ITEMS_PER_PAGE;
    const currentItems = inventory.slice(startIndex, endIndex);

    const handlePageChange = (page: number) => {
        setCurrentPage(page);
    };

    // Generate page numbers for pagination
    const getPageNumbers = () => {
        const pages = [];
        const maxVisible = 5;
        let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
        let endPage = Math.min(totalPages, startPage + maxVisible - 1);

        if (endPage - startPage < maxVisible - 1) {
            startPage = Math.max(1, endPage - maxVisible + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(i);
        }
        return pages;
    };

    if (loading) {
        return (
            <div className="text-center p-3">
                <Spinner animation="border" size="sm" className="me-2" />
                Loading inventory...
            </div>
        );
    }

    if (error) {
        return <div className="text-danger p-3">Error: {error}</div>;
    }

    if (inventory.length === 0) {
        return <div className="text-muted p-3">No inventory items in this warehouse</div>;
    }

    return (
        <div>
            <Table striped bordered hover size="sm" className="mb-2">
                <thead>
                    <tr>
                        <th>SKU</th>
                        <th>Game Title</th>
                        <th>Quantity</th>
                    </tr>
                </thead>
                <tbody>
                    {currentItems.map((wi) => (
                        <tr key={`${wi.warehouseId}-${wi.item.itemId}`}>
                            <td>{wi.item.sku}</td>
                            <td>{wi.item.gameTitle}</td>
                            <td>{wi.quantity}</td>
                        </tr>
                    ))}
                </tbody>
            </Table>
            {totalPages > 1 && (
                <div className="d-flex justify-content-between align-items-center">
                    <div className="text-muted">
                        Showing {startIndex + 1} to {Math.min(endIndex, inventory.length)} of {inventory.length} items
                    </div>
                    <Pagination className="mb-0">
                        <Pagination.First 
                            onClick={() => handlePageChange(1)} 
                            disabled={currentPage === 1}
                        />
                        <Pagination.Prev 
                            onClick={() => handlePageChange(currentPage - 1)} 
                            disabled={currentPage === 1}
                        />
                        {getPageNumbers().map((page) => (
                            <Pagination.Item
                                key={page}
                                active={page === currentPage}
                                onClick={() => handlePageChange(page)}
                            >
                                {page}
                            </Pagination.Item>
                        ))}
                        <Pagination.Next 
                            onClick={() => handlePageChange(currentPage + 1)} 
                            disabled={currentPage === totalPages}
                        />
                        <Pagination.Last 
                            onClick={() => handlePageChange(totalPages)} 
                            disabled={currentPage === totalPages}
                        />
                    </Pagination>
                </div>
            )}
        </div>
    );
}

