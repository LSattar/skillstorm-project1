import React, { useState } from 'react';
import { CompanyTable } from '../components/CompanyTable';
import { CompanyModal } from '../components/CompanyModal';
import { Button } from 'react-bootstrap';
import { Container, Row, Col } from 'react-bootstrap';

export const Companies = () => {
    const [showModal, setShowModal] = useState(false);
    const [editingCompanyId, setEditingCompanyId] = useState<number | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);

    const handleSave = async (company: { name: string; phone: string; email: string; contactPerson: string }) => {
        const isEditing = editingCompanyId !== null;
        const url = isEditing 
            ? `http://localhost:8080/company/${editingCompanyId}`
            : 'http://localhost:8080/company';
        
        const response = await fetch(url, {
            method: isEditing ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                id: isEditing ? editingCompanyId : null,
                name: company.name,
                phone: company.phone || null,
                email: company.email || null,
                contactPerson: company.contactPerson || null
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Failed to ${isEditing ? 'update' : 'create'} company` }));
            throw new Error(errorData.message || `Failed to ${isEditing ? 'update' : 'create'} company`);
        }

        setEditingCompanyId(null);
        setRefreshKey(prev => prev + 1);
    };

    const handleEdit = (companyId: number) => {
        setEditingCompanyId(companyId);
        setShowModal(true);
    };

    const handleDelete = async (companyId: number, companyName: string) => {
        if (!window.confirm(`Are you sure you want to delete "${companyName}"? This action cannot be undone.`)) {
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/company/${companyId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Failed to delete company' }));
                throw new Error(errorData.message || 'Failed to delete company');
            }

            setRefreshKey(prev => prev + 1);
        } catch (err: any) {
            alert('Error deleting company: ' + (err.message || 'Unknown error'));
            console.error('Error deleting company:', err);
        }
    };

    const handleCloseModal = () => {
        setShowModal(false);
        setEditingCompanyId(null);
    };

    const handleAddNew = () => {
        setEditingCompanyId(null);
        setShowModal(true);
    };

    return (
        <Container className='mt-3'>
            <Row>
                <Col className='text-start'><h1>Companies</h1></Col>
                <Col className="text-end">
                    <Button onClick={handleAddNew}>
                        <i className="bi bi-plus-circle"></i> Add Company
                    </Button>
                </Col>
            </Row>
            <Row>
                <CompanyTable key={refreshKey} onEdit={handleEdit} onDelete={handleDelete} />
            </Row>
            <CompanyModal 
                show={showModal} 
                onHide={handleCloseModal}
                companyId={editingCompanyId}
                onSave={handleSave}
            />
        </Container>
    );
}
