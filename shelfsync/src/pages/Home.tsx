import React, { useEffect, useState } from 'react';
import { Container, Row, Col } from 'react-bootstrap';
import { WarehouseCard } from '../components/WarehouseCard';

interface WarehouseData {
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

interface CapacityData {
    warehouseId: number;
    maximumCapacityCubicFeet: number;
    usedCapacityCubicFeet: number;
    availableCapacityCubicFeet: number;
    utilizationPercent: number;
}

export const Home = () => {
    const [warehouses, setWarehouses] = useState<WarehouseData[]>([]);
    const [capacities, setCapacities] = useState<CapacityData[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                
                // Fetch warehouses and capacities in parallel
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
                setCapacities(capacitiesData);
                setError(null);
            } catch (err: any) {
                setError('Failed to load warehouses: ' + (err.message || 'Unknown error'));
                console.error('Error fetching warehouses:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    if (loading) {
        return (
            <Container className="mt-3">
                <h1>Home Page/Dashboard</h1>
                <div>Loading warehouses...</div>
            </Container>
        );
    }

    if (error) {
        return (
            <Container className="mt-3">
                <h1>Home Page/Dashboard</h1>
                <div className="text-danger">Error: {error}</div>
            </Container>
        );
    }

    const warehousesWithCapacity = warehouses.map(warehouse => {
        const capacity = capacities.find(c => c.warehouseId === warehouse.id);
        const managerName = warehouse.manager 
            ? `${warehouse.manager.firstName} ${warehouse.manager.lastName}`
            : null;
        
        return {
            ...warehouse,
            capacityPercent: capacity ? capacity.utilizationPercent : 0,
            managerName
        };
    });

    return (
        <Container className="mt-3">
            <Row className='text-start'>
                <h1>Admin Dashboard</h1>
            </Row>
            <Row>
                {warehousesWithCapacity.length === 0 ? (
                    <Col>
                        <p>No warehouses found</p>
                    </Col>
                ) : (
                    warehousesWithCapacity.map((warehouse) => (
                        <Col key={warehouse.id} md={6} lg={4}>
                            <WarehouseCard
                                warehouseId={warehouse.id}
                                name={warehouse.name}
                                address={warehouse.address}
                                city={warehouse.city}
                                state={warehouse.state}
                                zip={warehouse.zip}
                                managerName={warehouse.managerName}
                                capacityPercent={warehouse.capacityPercent}
                            />
                        </Col>
                    ))
                )}
            </Row>
        </Container>
    );
}