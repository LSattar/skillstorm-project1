import React, { useState, useEffect } from 'react';
import { Table, Pagination, ButtonGroup, Button } from 'react-bootstrap';

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

interface InventoryTableProps {
    items: InventoryItem[];
    loading: boolean;
    error: string | null;
    onEdit?: (itemId: number) => void;
    onDelete?: (itemId: number, itemName: string) => void;
}

const ITEMS_PER_PAGE = 10;

export const InventoryTable: React.FC<InventoryTableProps> = ({ items, loading, error, onEdit, onDelete }) => {
    const [currentPage, setCurrentPage] = useState<number>(1);

    // Reset to first page when items change
    useEffect(() => {
        setCurrentPage(1);
    }, [items]);

    // Calculate pagination
    const totalPages = Math.ceil(items.length / ITEMS_PER_PAGE);
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const endIndex = startIndex + ITEMS_PER_PAGE;
    const currentItems = items.slice(startIndex, endIndex);

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
        return <div>Loading inventory...</div>;
    }

    if (error) {
        return <div className="text-danger">Error: {error}</div>;
    }

    if (items.length === 0) {
        return <div className="text-muted">No inventory items found</div>;
    }

    return (
        <div>
            <Table striped bordered hover className="mb-2">
                <thead>
                    <tr>
                        <th>SKU</th>
                        <th>Game Title</th>
                        <th>Category</th>
                        <th>Total Quantity</th>
                        <th>Locations</th>
                        {(onEdit || onDelete) && <th>Actions</th>}
                    </tr>
                </thead>
                <tbody>
                    {currentItems.map((item) => {
                        const isZeroQuantity = item.totalQuantity === 0;
                        return (
                            <tr 
                                key={item.itemId}
                            >
                                <td>{item.sku}</td>
                                <td>{item.gameTitle}</td>
                                <td>{item.category || '-'}</td>
                                <td>
                                    {isZeroQuantity ? (
                                        <span className="text-danger">{item.totalQuantity}</span>
                                    ) : (
                                        item.totalQuantity
                                    )}
                                </td>
                                <td>
                                    {item.locations.length === 0 ? (
                                        <span className="text-muted">No locations</span>
                                    ) : (
                                        <ul className="mb-0">
                                            {item.locations.map((location, index) => (
                                                <li style={{ listStyleType: 'none' }} key={index}>
                                                    {location.warehouseName}: {location.quantity}
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </td>
                                {(onEdit || onDelete) && (
                                    <td>
                                        <ButtonGroup size="sm">
                                            {onEdit && (
                                                <Button 
                                                    variant="outline-primary" 
                                                    onClick={() => onEdit(item.itemId)} 
                                                    title="Edit item"
                                                >
                                                    <i className="bi bi-pencil"></i>
                                                </Button>
                                            )}
                                            {onDelete && (
                                                <Button 
                                                    variant="outline-danger" 
                                                    onClick={() => onDelete(item.itemId, item.gameTitle)} 
                                                    title="Delete item"
                                                >
                                                    <i className="bi bi-trash"></i>
                                                </Button>
                                            )}
                                        </ButtonGroup>
                                    </td>
                                )}
                            </tr>
                        );
                    })}
                </tbody>
            </Table>
            {totalPages > 1 && (
                <div className="d-flex justify-content-between align-items-center">
                    <div className="text-muted">
                        Showing {startIndex + 1} to {Math.min(endIndex, items.length)} of {items.length} items
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

