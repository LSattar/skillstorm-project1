import React from 'react';
import { Card, ProgressBar } from 'react-bootstrap';

interface WarehouseCardProps {
    warehouseId: number;
    name: string;
    address: string | null;
    city: string | null;
    state: string | null;
    zip: string | null;
    managerName?: string | null;
    capacityPercent: number;
}

export const WarehouseCard: React.FC<WarehouseCardProps> = ({
    warehouseId,
    name,
    address,
    city,
    state,
    zip,
    managerName,
    capacityPercent
}) => {
    const getCapacityVariant = () => {
        if (capacityPercent >= 90) return 'danger';
        if (capacityPercent >= 75) return 'warning';
        return 'success';
    };

    const location = [address, city, state, zip].filter(Boolean).join(', ') || 'No address';

    return (
        <Card className="mb-3">
            <Card.Header>
                <Card.Title className="mb-0">{name}</Card.Title>
            </Card.Header>
            <Card.Body>
                <p className="mb-2"><strong>Location:</strong> {location}</p>
                {managerName && <p className="mb-2"><strong>Manager:</strong> {managerName}</p>}
                <div className="mt-3">
                    <div className="d-flex justify-content-between mb-2">
                        <span><strong>Capacity:</strong></span>
                        <span>{capacityPercent.toFixed(1)}%</span>
                    </div>
                    <ProgressBar 
                        now={capacityPercent} 
                        variant={getCapacityVariant()}
                        label={`${capacityPercent.toFixed(1)}%`}
                    />
                </div>
            </Card.Body>
        </Card>
    );
}