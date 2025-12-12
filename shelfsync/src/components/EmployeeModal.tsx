import React, { useState, useEffect } from 'react';
import { Modal, Button, Form } from 'react-bootstrap';

interface Warehouse {
    id: number;
    name: string;
}

interface EmployeeModalProps {
    show: boolean;
    onHide: () => void;
    employeeId?: string | null;
    onSave: (employee: { 
        firstName: string; 
        lastName: string; 
        phone: string; 
        email: string; 
        assignedWarehouseId: number | null 
    }) => Promise<void>;
}

export const EmployeeModal: React.FC<EmployeeModalProps> = ({ show, onHide, employeeId, onSave }) => {
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [phone, setPhone] = useState('');
    const [email, setEmail] = useState('');
    const [assignedWarehouseId, setAssignedWarehouseId] = useState<number | null>(null);
    const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [loadingEmployee, setLoadingEmployee] = useState(false);

    useEffect(() => {
        if (show) {
            const fetchData = async () => {
                try {
                    const warehousesResponse = await fetch('http://localhost:8080/warehouse');
                    if (warehousesResponse.ok) {
                        const warehousesData = await warehousesResponse.json();
                        setWarehouses(warehousesData);
                    }

                    if (employeeId) {
                        setLoadingEmployee(true);
                        const employeeResponse = await fetch(`http://localhost:8080/employee/${employeeId}`);
                        if (employeeResponse.ok) {
                            const employeeData = await employeeResponse.json();
                            setFirstName(employeeData.firstName || '');
                            setLastName(employeeData.lastName || '');
                            setPhone(employeeData.phone || '');
                            setEmail(employeeData.email || '');
                            setAssignedWarehouseId(employeeData.assignedWarehouse?.id || null);
                        }
                    } else {
                        setFirstName('');
                        setLastName('');
                        setPhone('');
                        setEmail('');
                        setAssignedWarehouseId(null);
                    }
                } catch (err) {
                    console.error('Error fetching data:', err);
                    setError('Failed to load employee data');
                } finally {
                    setLoadingEmployee(false);
                }
            };
            fetchData();
        } else {
            setFirstName('');
            setLastName('');
            setPhone('');
            setEmail('');
            setAssignedWarehouseId(null);
            setError(null);
        }
    }, [show, employeeId]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!firstName.trim() || !lastName.trim() || !phone.trim()) {
            setError('First name, last name, and phone are required');
            return;
        }

        try {
            setLoading(true);
            setError(null);
            await onSave({
                firstName: firstName.trim(),
                lastName: lastName.trim(),
                phone: phone.trim(),
                email: email.trim() || '',
                assignedWarehouseId: assignedWarehouseId || null
            });
            if (!employeeId) {
                setFirstName('');
                setLastName('');
                setPhone('');
                setEmail('');
                setAssignedWarehouseId(null);
            }
            onHide();
        } catch (err: any) {
            setError(err.message || 'Failed to create employee');
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        setFirstName('');
        setLastName('');
        setPhone('');
        setEmail('');
        setAssignedWarehouseId(null);
        setError(null);
        onHide();
    };

    return (
        <Modal show={show} onHide={handleClose}>
            <Modal.Header closeButton>
                <Modal.Title>{employeeId ? 'Edit Employee' : 'Add New Employee'}</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {error && <div className="alert alert-danger">{error}</div>}
                    <Form.Group className="mb-3">
                        <Form.Label>First Name <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="text"
                            value={firstName}
                            onChange={(e) => setFirstName(e.target.value)}
                            required
                            placeholder="Enter first name"
                        />
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Last Name <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="text"
                            value={lastName}
                            onChange={(e) => setLastName(e.target.value)}
                            required
                            placeholder="Enter last name"
                        />
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Phone <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="tel"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
                            required
                            placeholder="Enter phone number"
                        />
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Email</Form.Label>
                        <Form.Control
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="Enter email address"
                        />
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Assigned Warehouse</Form.Label>
                        <Form.Select
                            value={assignedWarehouseId || ''}
                            onChange={(e) => setAssignedWarehouseId(e.target.value ? parseInt(e.target.value) : null)}
                            disabled={loadingEmployee}
                        >
                            <option value="">{loadingEmployee ? 'Loading...' : 'None'}</option>
                            {warehouses.map((warehouse) => (
                                <option key={warehouse.id} value={warehouse.id}>
                                    {warehouse.name}
                                </option>
                            ))}
                        </Form.Select>
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose} disabled={loading}>
                        Cancel
                    </Button>
                    <Button variant="primary" type="submit" disabled={loading || loadingEmployee}>
                        {loading ? 'Saving...' : employeeId ? 'Update Employee' : 'Save Employee'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
}

