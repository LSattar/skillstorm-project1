import React, { useEffect, useState } from 'react';
import { Container, Row, Col, Accordion, Button, ButtonGroup } from 'react-bootstrap';
import { WarehouseInventory } from '../components/WarehouseInventory';
import { WarehouseActivity } from '../components/WarehouseActivity';
import { WarehouseModal } from '../components/WarehouseModal';

interface Warehouse {
    id: number;
    name: string;
    address: string | null;
    city: string | null;
    state: string | null;
    zip: string | null;
    manager: {
        firstName: string;
        lastName: string;
    } | null;
    maximumCapacityCubicFeet: number;
}

interface WarehouseCapacity {
    warehouseId: number;
    maximumCapacityCubicFeet: number;
    usedCapacityCubicFeet: number;
    availableCapacityCubicFeet: number;
    utilizationPercent: number;
}

export const Warehouses = () => {
    const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
    const [capacities, setCapacities] = useState<Map<number, WarehouseCapacity>>(new Map());
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [showModal, setShowModal] = useState(false);
    const [editingWarehouseId, setEditingWarehouseId] = useState<number | null>(null);
    const [refreshKey, setRefreshKey] = useState(0);

    const fetchData = async () => {
        try {
            setLoading(true);
            setError(null);
            
            const [warehousesResponse, capacitiesResponse] = await Promise.all([
                fetch('http://localhost:8080/warehouse'),
                fetch('http://localhost:8080/warehouse/capacity')
            ]);

            if (!warehousesResponse.ok || !capacitiesResponse.ok) {
                throw new Error('Failed to fetch warehouse data');
            }

            const warehousesData = await warehousesResponse.json();
            const capacitiesData = await capacitiesResponse.json();

            setWarehouses(warehousesData);
            
            const capacityMap = new Map<number, WarehouseCapacity>();
            capacitiesData.forEach((cap: WarehouseCapacity) => {
                capacityMap.set(cap.warehouseId, cap);
            });
            setCapacities(capacityMap);
        } catch (err: any) {
            setError('Failed to load warehouses: ' + (err.message || 'Unknown error'));
            console.error('Error fetching warehouses:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    useEffect(() => {
        if (refreshKey > 0) {
            fetchData();
        }
    }, [refreshKey]);

    const getCapacityPercent = (warehouseId: number): number => {
        const capacity = capacities.get(warehouseId);
        return capacity ? capacity.utilizationPercent : 0;
    };

    const getLocationString = (warehouse: Warehouse): string => {
        const parts = [warehouse.address, warehouse.city, warehouse.state, warehouse.zip].filter(Boolean);
        return parts.length > 0 ? parts.join(', ') : 'No address';
    };

    const handleSave = async (warehouse: {
        name: string;
        address: string | null;
        city: string | null;
        state: string | null;
        zip: string | null;
        managerEmployeeId: string | null;
        maximumCapacityCubicFeet: number;
    }) => {
        const isEditing = editingWarehouseId !== null;
        const url = isEditing 
            ? `http://localhost:8080/warehouse/${editingWarehouseId}`
            : 'http://localhost:8080/warehouse';
        
        const response = await fetch(url, {
            method: isEditing ? 'PUT' : 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                id: isEditing ? editingWarehouseId : null,
                name: warehouse.name,
                address: warehouse.address,
                city: warehouse.city,
                state: warehouse.state,
                zip: warehouse.zip,
                managerEmployeeId: warehouse.managerEmployeeId,
                maximumCapacityCubicFeet: warehouse.maximumCapacityCubicFeet
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Failed to ${isEditing ? 'update' : 'create'} warehouse` }));
            throw new Error(errorData.message || `Failed to ${isEditing ? 'update' : 'create'} warehouse`);
        }

        setEditingWarehouseId(null);
        setRefreshKey(prev => prev + 1);
    };

    const handleEdit = (warehouseId: number) => {
        setEditingWarehouseId(warehouseId);
        setShowModal(true);
    };

    const handleDelete = async (warehouseId: number, warehouseName: string) => {
        if (!window.confirm(`Are you sure you want to delete "${warehouseName}"? This action cannot be undone.`)) {
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/warehouse/${warehouseId}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Failed to delete warehouse' }));
                throw new Error(errorData.message || 'Failed to delete warehouse');
            }

            setRefreshKey(prev => prev + 1);
        } catch (err: any) {
            alert('Error deleting warehouse: ' + (err.message || 'Unknown error'));
            console.error('Error deleting warehouse:', err);
        }
    };

    const handleCloseModal = () => {
        setShowModal(false);
        setEditingWarehouseId(null);
    };

    const handleAddNew = () => {
        setEditingWarehouseId(null);
        setShowModal(true);
    };

    if (loading) {
        return (
            <Container className="mt-3">
                <h1>Warehouses</h1>
                <div>Loading warehouses...</div>
            </Container>
        );
    }

    if (error) {
        return (
            <Container className="mt-3">
                <h1>Warehouses</h1>
                <div className="text-danger">Error: {error}</div>
            </Container>
        );
    }

    return (
        <Container className="mt-3">
            <Row>
                <Col><h1>Warehouses</h1></Col>
                <Col className="text-end">
                    <Button onClick={handleAddNew}>
                        <i className="bi bi-plus-circle me-1"></i>
                        Add Warehouse
                    </Button>
                </Col>
            </Row>
            <Row>
                <Col>
                    {warehouses.length === 0 ? (
                        <div className="text-muted">No warehouses found</div>
                    ) : (
                        <Accordion>
                            {warehouses.map((warehouse, index) => {
                                const capacityPercent = getCapacityPercent(warehouse.id);
                                const managerName = warehouse.manager 
                                    ? `${warehouse.manager.firstName} ${warehouse.manager.lastName}`
                                    : '-';
                                
                                return (
                                    <Accordion.Item eventKey={index.toString()} key={warehouse.id}>
                                        <Accordion.Header>
                                            <div className="d-flex justify-content-between align-items-center w-100 me-3">
                                                <div>
                                                    <strong>{warehouse.name}</strong>
                                                    <span className="text-muted ms-3">
                                                        {getLocationString(warehouse)}
                                                    </span>
                                                    <span className="text-muted ms-3">
                                                        Manager: {managerName}
                                                    </span>
                                                </div>
                                                <div className="d-flex align-items-center gap-2">
                                                    <span className={`badge ${capacityPercent >= 90 ? 'bg-danger' : capacityPercent >= 75 ? 'bg-warning' : 'bg-success'}`}>
                                                        {capacityPercent.toFixed(1)}% Capacity
                                                    </span>
                                                    <ButtonGroup size="sm" onClick={(e) => e.stopPropagation()}>
                                                        <Button 
                                                            variant="outline-primary" 
                                                            onClick={() => handleEdit(warehouse.id)}
                                                            title="Edit warehouse"
                                                        >
                                                            <i className="bi bi-pencil"></i>
                                                        </Button>
                                                        <Button 
                                                            variant="outline-danger" 
                                                            onClick={() => handleDelete(warehouse.id, warehouse.name)}
                                                            title="Delete warehouse"
                                                        >
                                                            <i className="bi bi-trash"></i>
                                                        </Button>
                                                    </ButtonGroup>
                                                </div>
                                            </div>
                                        </Accordion.Header>
                                        <Accordion.Body>
                                            <Container className="mb-3">
                                                <Row>
                                                    <Col><strong>Maximum Capacity:</strong> {warehouse.maximumCapacityCubicFeet} cubic feet</Col>
                                                    {capacities.has(warehouse.id) && (
                                                        <>
                                                            <Col><strong>Used Capacity:</strong> {capacities.get(warehouse.id)!.usedCapacityCubicFeet} cubic feet</Col>
                                                            <Col><strong>Available Capacity:</strong> {capacities.get(warehouse.id)!.availableCapacityCubicFeet} cubic feet</Col>
                                                        </>
                                                    )}
                                                </Row>
                                            </Container>
                                            <div className="mb-4">
                                                <h5>Inventory</h5>
                                                <WarehouseInventory warehouseId={warehouse.id} />
                                            </div>
                                            <div>
                                                <h5>Activity</h5>
                                                <WarehouseActivity warehouseId={warehouse.id} />
                                            </div>
                                        </Accordion.Body>
                                    </Accordion.Item>
                                );
                            })}
                        </Accordion>
                    )}
                </Col>
            </Row>
            <WarehouseModal
                show={showModal}
                onHide={handleCloseModal}
                warehouseId={editingWarehouseId}
                onSave={handleSave}
            />
        </Container>
    );
}
