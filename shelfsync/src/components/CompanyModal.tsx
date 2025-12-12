import React, { useState, useEffect } from 'react';
import { Modal, Button, Form } from 'react-bootstrap';

interface CompanyModalProps {
    show: boolean;
    onHide: () => void;
    companyId?: number | null; 
    onSave: (company: { name: string; phone: string; email: string; contactPerson: string }) => Promise<void>;
}

export const CompanyModal: React.FC<CompanyModalProps> = ({ show, onHide, companyId, onSave }) => {
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [email, setEmail] = useState('');
    const [contactPerson, setContactPerson] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [loadingCompany, setLoadingCompany] = useState(false);

    useEffect(() => {
        if (show) {
            if (companyId) {
                const fetchCompany = async () => {
                    try {
                        setLoadingCompany(true);
                        const response = await fetch(`http://localhost:8080/company/${companyId}`);
                        if (response.ok) {
                            const companyData = await response.json();
                            setName(companyData.name || '');
                            setPhone(companyData.phone || '');
                            setEmail(companyData.email || '');
                            setContactPerson(companyData.contactPerson || '');
                        }
                    } catch (err) {
                        console.error('Error fetching company:', err);
                        setError('Failed to load company data');
                    } finally {
                        setLoadingCompany(false);
                    }
                };
                fetchCompany();
            } else {
                setName('');
                setPhone('');
                setEmail('');
                setContactPerson('');
            }
        } else {
            setName('');
            setPhone('');
            setEmail('');
            setContactPerson('');
            setError(null);
        }
    }, [show, companyId]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!name.trim()) {
            setError('Company name is required');
            return;
        }

        try {
            setLoading(true);
            setError(null);
            await onSave({
                name: name.trim(),
                phone: phone.trim() || '',
                email: email.trim() || '',
                contactPerson: contactPerson.trim() || ''
            });
            if (!companyId) {
                setName('');
                setPhone('');
                setEmail('');
                setContactPerson('');
            }
            onHide();
        } catch (err: any) {
            setError(err.message || 'Failed to create company');
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        setName('');
        setPhone('');
        setEmail('');
        setContactPerson('');
        setError(null);
        onHide();
    };

    return (
        <Modal show={show} onHide={handleClose}>
            <Modal.Header closeButton>
                <Modal.Title>{companyId ? 'Edit Company' : 'Add New Company'}</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {error && <div className="alert alert-danger">{error}</div>}
                    <Form.Group className="mb-3">
                        <Form.Label>Company Name <span className="text-danger">*</span></Form.Label>
                        <Form.Control
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                            placeholder="Enter company name"
                        />
                    </Form.Group>
                    <Form.Group className="mb-3">
                        <Form.Label>Phone</Form.Label>
                        <Form.Control
                            type="tel"
                            value={phone}
                            onChange={(e) => setPhone(e.target.value)}
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
                        <Form.Label>Contact Person</Form.Label>
                        <Form.Control
                            type="text"
                            value={contactPerson}
                            onChange={(e) => setContactPerson(e.target.value)}
                            placeholder="Enter contact person name"
                        />
                    </Form.Group>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose} disabled={loading}>
                        Cancel
                    </Button>
                    <Button variant="primary" type="submit" disabled={loading || loadingCompany}>
                        {loading ? 'Saving...' : companyId ? 'Update Company' : 'Save Company'}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
}