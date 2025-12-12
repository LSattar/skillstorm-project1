import React, { useState, useEffect } from 'react';
import { Modal, Button, Form } from 'react-bootstrap';

interface Employee {
    id: string; // UUID as string
    firstName: string;
    lastName: string;
}

interface WarehouseModalProps {
    show: boolean;
    onHide: () => void;
    warehouseId?: number | null; // If provided, we're editing; otherwise creating
    onSave: (warehouse: {
        name: string;
        address: string | null;
        city: string | null;
        state: string | null;
        zip: string | null;
        managerEmployeeId: string | null;
        maximumCapacityCubicFeet: number;
    }) => Promise<void>;
}

export const WarehouseModal: React.FC<WarehouseModalProps> = ({ show, onHide, warehouseId, onSave }) => {
    const [name, setName] = useState('');
    const [address, setAddress] = useState('');
    const [city, setCity] = useState('');
    const [state, setState] = useState('');
    const [zip, setZip] = useState('');
    const [managerEmployeeId, setManagerEmployeeId] = useState<string | ''>('');
    const [maximumCapacityCubicFeet, setMaximumCapacityCubicFeet] = useState<number | ''>('');
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [loadingWarehouse, setLoadingWarehouse] = useState(false);

    useEffect(() => {
        if (show) {
            const fetchData = async () => {
                try {
                    const employeesResponse = await fetch('http://localhost:8080/employee');
                    if (employeesResponse.ok) {
                        const employeesData = await employeesResponse.json();
                        setEmployees(employeesData);
                    }

                    if (warehouseId) {
                        setLoadingWarehouse(true);
                        const warehouseResponse = await fetch(`http://localhost:8080/warehouse/${warehouseId}`);
                        if (warehouseResponse.ok) {
                            const warehouseData = await warehouseResponse.json();
                            setName(warehouseData.name || '');
                            setAddress(warehouseData.address || '');
                            setCity(warehouseData.city || '');
                            setState(warehouseData.state || '');
                            setZip(warehouseData.zip || '');
                            setManagerEmployeeId(warehouseData.manager?.id || '');
                            setMaximumCapacityCubicFeet(warehouseData.maximumCapacityCubicFeet || '');
                        }
                    }
                } catch (err) {
                    console.error('Error fetching data:', err);
                    setError('Failed to load warehouse data');
                } finally {
                    setLoadingWarehouse(false);
                }
            };
            fetchData();
        } else {
            setName('');
            setAddress('');
            setCity('');
            setState('');
            setZip('');
            setManagerEmployeeId('');
            setMaximumCapacityCubicFeet('');
            setError(null);
        }
    }, [show, warehouseId]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!name.trim()) {
            setError('Warehouse name is required');
            return;
        }

        if (!maximumCapacityCubicFeet || Number(maximumCapacityCubicFeet) <= 0) {
            setError('Maximum capacity must be greater than 0');
            return;
        }

        try {
            setLoading(true);
            setError(null);
            await onSave({
                name: name.trim(),
                address: address.trim() || null,
                city: city.trim() || null,
                state: state.trim() || null,
                zip: zip.trim() || null,
                managerEmployeeId: managerEmployeeId || null,
                maximumCapacityCubicFeet: Number(maximumCapacityCubicFeet)
            });
            // Reset form on success
            handleClose();
        } catch (err: any) {
            setError(err.message || 'Failed to create warehouse');
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        setName('');
        setAddress('');
        setCity('');
        setState('');
        setZip('');
        setManagerEmployeeId('');
        setMaximumCapacityCubicFeet('');
        setError(null);
        onHide();
    };

    return (
        <Modal show={show} onHide={handleClose} size="lg">
            <Modal.Header closeButton>
                <Modal.Title>{warehouseId ? 'Edit Warehouse' : 'Add New Warehouse'}</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {error && <div className="alert alert-danger">{error}</div>}
                    <Form.Group className="mb-3">
                        <Form.Label>Warehouse Name <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                            placeholder="Enter warehouse name"
                        />
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Address</Form.Label>
                        <Form.Control
                            type="text"
                            value={address}
                            onChange={(e) => setAddress(e.target.value)}
                            placeholder="Enter street address"
                        />
                    </Form.Group>
                    <div className="row">
                        <div className="col-md-6">
                            <Form.Group className="mb-3">
                                <Form.Label>City</Form.Label>
                                <Form.Control
                                    type="text"
                                    value={city}
                                    onChange={(e) => setCity(e.target.value)}
                                    placeholder="Enter city"
                                />
                            </Form.Group>
                        </div>
                        <div className="col-md-3">
                            <Form.Group className="mb-3">
                                <Form.Label>State</Form.Label>
                                <Form.Control
                                    type="text"
                                    value={state}
                                    onChange={(e) => setState(e.target.value)}
                                    placeholder="State"
                                    maxLength={2}
                                />
                            </Form.Group>
                        </div>
                        <div className="col-md-3">
                            <Form.Group className="mb-3">
                                <Form.Label>ZIP Code</Form.Label>
                                <Form.Control
                                    type="text"
                                    value={zip}
                                    onChange={(e) => setZip(e.target.value)}
                                    placeholder="ZIP"
                                />
                            </Form.Group>
                        </div>
                    </div>
                    <Form.Group className="mb-3">
                        <Form.Label>Manager</Form.Label>
                        <Form.Select
                            value={managerEmployeeId}
                            onChange={(e) => setManagerEmployeeId(e.target.value || '')}
                            disabled={loadingWarehouse}
                        >
                            <option value="">{loadingWarehouse ? 'Loading...' : 'Select manager (optional)'}</option>
                            {employees.map((employee) => (
                                <option key={employee.id} value={employee.id}>
                                    {employee.firstName} {employee.lastName}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Maximum Capacity (cubic feet) <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="number"
                            value={maximumCapacityCubicFeet}
                            onChange={(e) => setMaximumCapacityCubicFeet(e.target.value ? Number(e.target.value) : '')}
                            required
                            placeholder="Enter maximum capacity"
                            min="0.01"
                            step="0.01"
                        />
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose} disabled={loading}>
                        Cancel
                    </Button>
                    <Button variant="primary" type="submit" disabled={loading || loadingWarehouse}>
                        {loading ? 'Saving...' : warehouseId ? 'Update Warehouse' : 'Save Warehouse'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
}

