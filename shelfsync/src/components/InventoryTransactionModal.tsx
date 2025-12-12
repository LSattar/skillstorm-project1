import React, { useState, useEffect } from 'react';
import { Modal, Button, Form } from 'react-bootstrap';

interface Item {
    id: number; // Backend returns 'id', not 'itemId'
    sku: string;
    gameTitle: string;
}

interface Warehouse {
    id: number;
    name: string;
}

interface Employee {
    id: string; // UUID as string
    firstName: string;
    lastName: string;
}

interface InventoryTransactionModalProps {
    show: boolean;
    onHide: () => void;
    onSave: (transaction: {
        itemId: number;
        fromWarehouseId: number | null;
        toWarehouseId: number | null;
        quantityChange: number;
        transactionType: string;
        reason: string | null;
        performedByEmployeeId: string | null;
    }) => Promise<void>;
}

const TRANSACTION_TYPES = [
    { value: 'INBOUND', label: 'Inbound' },
    { value: 'OUTBOUND', label: 'Outbound' },
    { value: 'TRANSFER', label: 'Transfer' }
];

export const InventoryTransactionModal: React.FC<InventoryTransactionModalProps> = ({ show, onHide, onSave }) => {
    const [itemId, setItemId] = useState<string>('');
    const [fromWarehouseId, setFromWarehouseId] = useState<string>('');
    const [toWarehouseId, setToWarehouseId] = useState<string>('');
    const [quantityChange, setQuantityChange] = useState<number | ''>('');
    const [transactionType, setTransactionType] = useState<string>('INBOUND');
    const [reason, setReason] = useState<string>('');
    const [performedByEmployeeId, setPerformedByEmployeeId] = useState<string>('');

    const [items, setItems] = useState<Item[]>([]);
    const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Load dropdown data when modal opens
    useEffect(() => {
        if (show) {
            const fetchData = async () => {
                try {
                    const [itemsResponse, warehousesResponse, employeesResponse] = await Promise.all([
                        fetch('http://localhost:8080/item'),
                        fetch('http://localhost:8080/warehouse'),
                        fetch('http://localhost:8080/employee')
                    ]);

                    if (itemsResponse.ok) {
                        const itemsData = await itemsResponse.json();
                        setItems(itemsData);
                    }
                    if (warehousesResponse.ok) {
                        const warehousesData = await warehousesResponse.json();
                        setWarehouses(warehousesData);
                    }
                    if (employeesResponse.ok) {
                        const employeesData = await employeesResponse.json();
                        setEmployees(employeesData);
                    }
                } catch (err) {
                    console.error('Error fetching dropdown data:', err);
                }
            };
            fetchData();
        }
    }, [show]);

    // Reset form when transaction type changes
    useEffect(() => {
        if (transactionType === 'INBOUND') {
            setFromWarehouseId('');
        } else if (transactionType === 'OUTBOUND') {
            setToWarehouseId('');
        } else if (transactionType === 'TRANSFER') {
            // Keep both warehouses
        }
    }, [transactionType]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (itemId === '' || quantityChange === '' || !transactionType) {
            setError('Item, quantity change, and transaction type are required');
            return;
        }

        // Validate based on transaction type
        if (transactionType === 'TRANSFER' && (fromWarehouseId === '' || toWarehouseId === '')) {
            setError('Both from and to warehouses are required for transfers');
            return;
        }

        if (transactionType === 'OUTBOUND' && fromWarehouseId === '') {
            setError('From warehouse is required for outbound transactions');
            return;
        }

        if (transactionType === 'INBOUND' && toWarehouseId === '') {
            setError('To warehouse is required for inbound transactions');
            return;
        }

        try {
            setLoading(true);
            setError(null);
            await onSave({
                itemId: itemId === '' ? 0 : Number(itemId),
                fromWarehouseId: fromWarehouseId === '' ? null : Number(fromWarehouseId),
                toWarehouseId: toWarehouseId === '' ? null : Number(toWarehouseId),
                quantityChange: Number(quantityChange),
                transactionType,
                reason: reason.trim() || null,
                performedByEmployeeId: performedByEmployeeId === '' ? null : performedByEmployeeId
            });
            // Reset form on success
            handleClose();
        } catch (err: any) {
            setError(err.message || 'Failed to create transaction');
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        setItemId('');
        setFromWarehouseId('');
        setToWarehouseId('');
        setQuantityChange('');
        setTransactionType('INBOUND');
        setReason('');
        setPerformedByEmployeeId('');
        setError(null);
        onHide();
    };

    return (
        <Modal show={show} onHide={handleClose} size="lg">
            <Modal.Header closeButton>
                <Modal.Title>Record Inventory Transaction</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {error && <div className="alert alert-danger">{error}</div>}
                    <Form.Group className="mb-3">
                        <Form.Label>Item <span className="text-danger">*</span></Form.Label>
                        <Form.Select
                            value={itemId}
                            onChange={(e) => setItemId(e.target.value)}
                            required
                        >
                            <option value="">Select an item</option>
                            {items.map((item) => (
                                <option key={item.id} value={String(item.id)}>
                                    {item.sku} - {item.gameTitle}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>

                    <Form.Group className="mb-3">
                        <Form.Label>Transaction Type <span className="text-danger">*</span></Form.Label>
                        <Form.Select
                            value={transactionType}
                            onChange={(e) => setTransactionType(e.target.value)}
                            required
                        >
                            {TRANSACTION_TYPES.map((type) => (
                                <option key={type.value} value={type.value}>
                                    {type.label}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>

                    {transactionType === 'TRANSFER' && (
                        <>
                            <Form.Group className="mb-3">
                                <Form.Label>From Warehouse <span className="text-danger">*</span></Form.Label>
                                <Form.Select
                                    value={fromWarehouseId}
                                    onChange={(e) => setFromWarehouseId(e.target.value)}
                                    required
                                >
                                    <option value="">Select warehouse</option>
                                    {warehouses.map((warehouse) => (
                                        <option key={warehouse.id} value={String(warehouse.id)}>
                                            {warehouse.name}
                                        </option>
                                    ))}
                                </Form.Select>
                            </Form.Group>
                            <Form.Group className="mb-3">
                                <Form.Label>To Warehouse <span className="text-danger">*</span></Form.Label>
                                <Form.Select
                                    value={toWarehouseId}
                                    onChange={(e) => setToWarehouseId(e.target.value)}
                                    required
                                >
                                    <option value="">Select warehouse</option>
                                    {warehouses
                                        .filter((w) => fromWarehouseId === '' || String(w.id) !== fromWarehouseId)
                                        .map((warehouse) => (
                                            <option key={warehouse.id} value={String(warehouse.id)}>
                                                {warehouse.name}
                                            </option>
                                        ))}
                                </Form.Select>
                            </Form.Group>
                        </>
                    )}

                    {transactionType === 'INBOUND' && (
                        <Form.Group className="mb-3">
                            <Form.Label>To Warehouse <span className="text-danger">*</span></Form.Label>
                            <Form.Select
                                value={toWarehouseId}
                                onChange={(e) => setToWarehouseId(e.target.value)}
                                required
                            >
                                <option value="">Select warehouse</option>
                                {warehouses.map((warehouse) => (
                                    <option key={warehouse.id} value={String(warehouse.id)}>
                                        {warehouse.name}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    )}

                    {transactionType === 'OUTBOUND' && (
                        <Form.Group className="mb-3">
                            <Form.Label>From Warehouse <span className="text-danger">*</span></Form.Label>
                            <Form.Select
                                value={fromWarehouseId}
                                onChange={(e) => setFromWarehouseId(e.target.value)}
                                required
                            >
                                <option value="">Select warehouse</option>
                                {warehouses.map((warehouse) => (
                                    <option key={warehouse.id} value={String(warehouse.id)}>
                                        {warehouse.name}
                                    </option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    )}

                    <Form.Group className="mb-3">
                        <Form.Label>
                            Quantity Change <span className="text-danger">*</span>
                            <small className="text-muted ms-2">
                                (Positive number)
                            </small>
                        </Form.Label>
                        <Form.Control
                            type="number"
                            value={quantityChange}
                            onChange={(e) => setQuantityChange(e.target.value ? Number(e.target.value) : '')}
                            required
                            placeholder="Enter quantity"
                            min={1}
                        />
                    </Form.Group>

                    <Form.Group className="mb-3">
                        <Form.Label>Reason</Form.Label>
                        <Form.Control
                            as="textarea"
                            rows={3}
                            value={reason}
                            onChange={(e) => setReason(e.target.value)}
                            placeholder="Enter reason for this transaction (optional)"
                        />
                    </Form.Group>

                    <Form.Group className="mb-3">
                        <Form.Label>Performed By Employee</Form.Label>
                        <Form.Select
                            value={performedByEmployeeId}
                            onChange={(e) => setPerformedByEmployeeId(e.target.value || '')}
                        >
                            <option value="">Select employee (optional)</option>
                            {employees.map((employee) => (
                                <option key={employee.id} value={employee.id}>
                                    {employee.firstName} {employee.lastName}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose} disabled={loading}>
                        Cancel
                    </Button>
                    <Button variant="primary" type="submit" disabled={loading}>
                        {loading ? 'Saving...' : 'Record Transaction'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
}

