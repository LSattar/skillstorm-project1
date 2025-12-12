import React, { useState } from 'react';
import { Container, Row, Col, Button } from 'react-bootstrap';
import { EmployeeTable } from '../components/EmployeeTable';
import { EmployeeModal } from '../components/EmployeeModal';

export const Employees = () => {
    const [showModal, setShowModal] = useState(false);
    const [editingEmployeeId, setEditingEmployeeId] = useState<string | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);

    const handleSave = async (employee: { 
        firstName: string; 
        lastName: string; 
        phone: string; 
        email: string; 
        assignedWarehouseId: number | null 
    }) => {
        const isEditing = editingEmployeeId !== null;
        const url = isEditing 
            ? `http://localhost:8080/employee/${editingEmployeeId}`
            : 'http://localhost:8080/employee';
        
        const response = await fetch(url, {
            method: isEditing ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                id: isEditing ? editingEmployeeId : null,
                firstName: employee.firstName,
                lastName: employee.lastName,
                phone: employee.phone,
                email: employee.email || null,
                assignedWarehouseId: employee.assignedWarehouseId
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Failed to ${isEditing ? 'update' : 'create'} employee` }));
            throw new Error(errorData.message || `Failed to ${isEditing ? 'update' : 'create'} employee`);
        }

        setEditingEmployeeId(null);
        setRefreshKey(prev => prev + 1);
    };

    const handleEdit = (employeeId: string) => {
        setEditingEmployeeId(employeeId);
        setShowModal(true);
    };

    const handleDelete = async (employeeId: string, employeeName: string) => {
        if (!window.confirm(`Are you sure you want to delete "${employeeName}"? This action cannot be undone.`)) {
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/employee/${employeeId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Failed to delete employee' }));
                throw new Error(errorData.message || 'Failed to delete employee');
            }

            setRefreshKey(prev => prev + 1);
        } catch (err: any) {
            alert('Error deleting employee: ' + (err.message || 'Unknown error'));
            console.error('Error deleting employee:', err);
        }
    };

    const handleCloseModal = () => {
        setShowModal(false);
        setEditingEmployeeId(null);
    };

    const handleAddNew = () => {
        setEditingEmployeeId(null);
        setShowModal(true);
    };

    return (
        <Container className="mt-3">
            <Row>
                <Col className='text-start'><h1>Employees</h1></Col>
                <Col className='text-end'>
                    <Button onClick={handleAddNew}>
                        <i className="bi bi-plus-circle"></i> Add Employee
                    </Button>
                </Col>
            </Row>
            <Row>
                <EmployeeTable key={refreshKey} onEdit={handleEdit} onDelete={handleDelete} />
            </Row>
            <EmployeeModal 
                show={showModal} 
                onHide={handleCloseModal}
                employeeId={editingEmployeeId}
                onSave={handleSave}
            />
        </Container>
    );
}
