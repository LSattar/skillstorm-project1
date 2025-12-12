import React, { useState, useEffect } from 'react';
import { Modal, Button, Form } from 'react-bootstrap';

interface Category {
    categoryId: number;
    categoryName: string;
}

interface Company {
    id: number;
    name: string;
}

interface ItemModalProps {
    show: boolean;
    onHide: () => void;
    itemId: number | null; // Added for editing
    onSave: (item: {
        sku: string;
        gameTitle: string;
        categoryId: number | null;
        companyId: number | null;
        weightLbs: number;
        cubicFeet: number;
    }) => Promise<void>;
}

export const ItemModal: React.FC<ItemModalProps> = ({ show, onHide, itemId, onSave }) => {
    const [sku, setSku] = useState('');
    const [gameTitle, setGameTitle] = useState('');
    const [categoryId, setCategoryId] = useState<number | ''>('');
    const [companyId, setCompanyId] = useState<number | ''>('');
    const [weightLbs, setWeightLbs] = useState<number | ''>('');
    const [cubicFeet, setCubicFeet] = useState<number | ''>('');
    const [categories, setCategories] = useState<Category[]>([]);
    const [companies, setCompanies] = useState<Company[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [dataLoading, setDataLoading] = useState(true);
    const [loadingItem, setLoadingItem] = useState(false);

    // Load categories, companies, and item data when modal opens
    useEffect(() => {
        if (show) {
            const fetchData = async () => {
                try {
                    setDataLoading(true);
                    setError(null);
                    
                    const [categoriesResponse, companiesResponse] = await Promise.all([
                        fetch('http://localhost:8080/category'),
                        fetch('http://localhost:8080/company')
                    ]);

                    if (categoriesResponse.ok) {
                        const categoriesData = await categoriesResponse.json();
                        setCategories(categoriesData);
                    }
                    if (companiesResponse.ok) {
                        const companiesData = await companiesResponse.json();
                        setCompanies(companiesData);
                    }

                    // If editing, fetch item data
                    if (itemId !== null) {
                        setLoadingItem(true);
                        const itemResponse = await fetch(`http://localhost:8080/item/${itemId}`);
                        if (itemResponse.ok) {
                            const itemData = await itemResponse.json();
                            setSku(itemData.sku || '');
                            setGameTitle(itemData.gameTitle || '');
                            setCategoryId(itemData.category?.categoryId || '');
                            setCompanyId(itemData.company?.id || '');
                            setWeightLbs(itemData.weightLbs || '');
                            setCubicFeet(itemData.cubicFeet || '');
                        } else {
                            throw new Error('Failed to fetch item data');
                        }
                    } else {
                        // Reset form for new item
                        setSku('');
                        setGameTitle('');
                        setCategoryId('');
                        setCompanyId('');
                        setWeightLbs('');
                        setCubicFeet('');
                    }
                } catch (err: any) {
                    setError('Failed to load data: ' + (err.message || 'Unknown error'));
                    console.error('Error fetching data for item modal:', err);
                } finally {
                    setDataLoading(false);
                    setLoadingItem(false);
                }
            };
            fetchData();
        }
    }, [show, itemId]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!sku.trim()) {
            setError('SKU is required');
            return;
        }

        if (!gameTitle.trim()) {
            setError('Game Title is required');
            return;
        }

        if (!weightLbs || Number(weightLbs) <= 0) {
            setError('Weight must be greater than 0');
            return;
        }

        if (!cubicFeet || Number(cubicFeet) <= 0) {
            setError('Cubic feet must be greater than 0');
            return;
        }

        try {
            setLoading(true);
            setError(null);
            await onSave({
                sku: sku.trim(),
                gameTitle: gameTitle.trim(),
                categoryId: categoryId ? Number(categoryId) : null,
                companyId: companyId ? Number(companyId) : null,
                weightLbs: Number(weightLbs),
                cubicFeet: Number(cubicFeet)
            });
            // Reset form on success
            handleClose();
        } catch (err: any) {
            setError(err.message || `Failed to ${itemId ? 'update' : 'create'} item`);
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        setSku('');
        setGameTitle('');
        setCategoryId('');
        setCompanyId('');
        setWeightLbs('');
        setCubicFeet('');
        setError(null);
        setDataLoading(true);
        setLoadingItem(false);
        onHide();
    };

    return (
        <Modal show={show} onHide={handleClose} size="lg">
            <Modal.Header closeButton>
                <Modal.Title>{itemId ? 'Edit Item' : 'Add New Item'}</Modal.Title>
            </Modal.Header>
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    {error && <div className="alert alert-danger">{error}</div>}
                    {(dataLoading || loadingItem) ? (
                        <div>Loading data...</div>
                    ) : (
                        <>
                            <Form.Group className="mb-3">
                                <Form.Label>SKU <span className="text-danger">*</span></Form.Label>
                                <Form.Control
                                    type="text"
                                    value={sku}
                                    onChange={(e) => setSku(e.target.value)}
                                    required
                                    placeholder="Enter SKU"
                                />
                            </Form.Group>
                            <Form.Group className="mb-3">
                                <Form.Label>Game Title <span className="text-danger">*</span></Form.Label>
                                <Form.Control
                                    type="text"
                                    value={gameTitle}
                                    onChange={(e) => setGameTitle(e.target.value)}
                                    required
                                    placeholder="Enter game title"
                                />
                            </Form.Group>
                            <div className="row">
                                <div className="col-md-6">
                                    <Form.Group className="mb-3">
                                        <Form.Label>Category</Form.Label>
                                        <Form.Select
                                            value={categoryId}
                                            onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}
                                        >
                                            <option value="">Select category</option>
                                            {categories.map((category) => (
                                                <option key={category.categoryId} value={category.categoryId}>
                                                    {category.categoryName}
                                                </option>
                                            ))}
                                        </Form.Select>
                                    </Form.Group>
                                </div>
                                <div className="col-md-6">
                                    <Form.Group className="mb-3">
                                        <Form.Label>Company</Form.Label>
                                        <Form.Select
                                            value={companyId}
                                            onChange={(e) => setCompanyId(e.target.value ? Number(e.target.value) : '')}
                                        >
                                            <option value="">Select company</option>
                                            {companies.map((company) => (
                                                <option key={company.id} value={company.id}>
                                                    {company.name}
                                                </option>
                                            ))}
                                        </Form.Select>
                                    </Form.Group>
                                </div>
                            </div>
                            <div className="row">
                                <div className="col-md-6">
                                    <Form.Group className="mb-3">
                                        <Form.Label>Weight (lbs) <span className="text-danger">*</span></Form.Label>
                                        <Form.Control
                                            type="number"
                                            value={weightLbs}
                                            onChange={(e) => setWeightLbs(e.target.value ? Number(e.target.value) : '')}
                                            required
                                            placeholder="Enter weight"
                                            min="0.01"
                                            step="0.01"
                                        />
                                    </Form.Group>
                                </div>
                                <div className="col-md-6">
                                    <Form.Group className="mb-3">
                                        <Form.Label>Cubic Feet <span className="text-danger">*</span></Form.Label>
                                        <Form.Control
                                            type="number"
                                            value={cubicFeet}
                                            onChange={(e) => setCubicFeet(e.target.value ? Number(e.target.value) : '')}
                                            required
                                            placeholder="Enter cubic feet"
                                            min="0.01"
                                            step="0.01"
                                        />
                                    </Form.Group>
                                </div>
                            </div>
                        </>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={handleClose} disabled={loading || dataLoading || loadingItem}>
                        Cancel
                    </Button>
                    <Button variant="primary" type="submit" disabled={loading || dataLoading || loadingItem}>
                        {loading ? 'Saving...' : (itemId ? 'Update Item' : 'Save Item')}
                    </Button>
                </Modal.Footer>
            </Form>
        </Modal>
    );
}

