import React, { useEffect, useState } from 'react';
import { Table, Spinner} from 'react-bootstrap';

interface WarehouseActivityProps {
    warehouseId: number;
}

interface InventoryHistory {
    id: number;
    itemId: number;
    fromWarehouseId: number | null;
    toWarehouseId: number | null;
    quantityChange: number;
    transactionType: string;
    reason: string | null;
    occurredAt: string;
    performedByEmployeeId: string | null;
}

interface Item {
    id: number;
    sku: string;
    gameTitle: string;
}

interface Employee {
    id: string;
    firstName: string;
    lastName: string;
}

export const WarehouseActivity: React.FC<WarehouseActivityProps> = ({ warehouseId }) => {
    const [activity, setActivity] = useState<InventoryHistory[]>([]);
    const [items, setItems] = useState<Map<number, Item>>(new Map());
    const [employees, setEmployees] = useState<Map<string, Employee>>(new Map());
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchActivity = async () => {
            try {
                setLoading(true);
                setError(null);
                
                const [activityResponse, itemsResponse, employeesResponse] = await Promise.all([
                    fetch(`http://localhost:8080/inventory-history/warehouse/${warehouseId}`),
                    fetch('http://localhost:8080/item'),
                    fetch('http://localhost:8080/employee')
                ]);
                
                if (!activityResponse.ok) {
                    throw new Error(`HTTP error! status: ${activityResponse.status}`);
                }

                const activityData = await activityResponse.json();
                setActivity(activityData);

                if (itemsResponse.ok) {
                    const itemsData = await itemsResponse.json();
                    const itemsMap = new Map<number, Item>();
                    itemsData.forEach((item: Item) => {
                        itemsMap.set(item.id, item);
                    });
                    setItems(itemsMap);
                }

                if (employeesResponse.ok) {
                    const employeesData = await employeesResponse.json();
                    const employeesMap = new Map<string, Employee>();
                    employeesData.forEach((emp: Employee) => {
                        employeesMap.set(emp.id, emp);
                    });
                    setEmployees(employeesMap);
                }
            } catch (err: any) {
                setError('Failed to load activity: ' + (err.message || 'Unknown error'));
                console.error('Error fetching warehouse activity:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchActivity();
    }, [warehouseId]);

    const formatDate = (dateString: string) => {
        try {
            const date = new Date(dateString);
            return date.toLocaleString();
        } catch {
            return dateString;
        }
    };

    if (loading) {
        return (
            <div className="text-center p-3">
                <Spinner animation="border" size="sm" className="me-2" />
                Loading activity...
            </div>
        );
    }

    if (error) {
        return <div className="text-danger p-3">Error: {error}</div>;
    }

    if (activity.length === 0) {
        return <div className="text-muted p-3">No activity recorded for this warehouse</div>;
    }

    return (
        <div>
            <Table striped bordered hover size="sm">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Quantity Change</th>
                        <th>Type</th>
                        <th>Item</th>
                        <th>Performed By</th>
                        <th>Reason</th>
                    </tr>
                </thead>
                <tbody>
                    {activity.map((history) => {
                        const item = items.get(history.itemId);
                        const itemName = item ? item.gameTitle : `Item #${history.itemId}`;
                        return (
                            <tr key={history.id}>
                                <td>{formatDate(history.occurredAt)}</td>
                                <td>
                                    {(() => {
                                        let displayValue = '';
                                        let className = '';
                                        
                                        if (history.transactionType === 'OUTBOUND') {
                                            displayValue = `-${Math.abs(history.quantityChange)}`;
                                            className = 'text-danger';
                                        } else if (history.transactionType === 'INBOUND') {
                                            displayValue = `+${Math.abs(history.quantityChange)}`;
                                            className = 'text-success';
                                        } else {
                                            // TRANSFER - no sign prefix
                                            displayValue = String(history.quantityChange);
                                            className = history.quantityChange > 0 ? 'text-success' : 'text-danger';
                                        }
                                        
                                        return (
                                            <span className={className}>
                                                {displayValue}
                                            </span>
                                        );
                                    })()}
                                </td>
                                <td>
                                {history.transactionType}
                                </td>
                                <td>{itemName}</td>
                                <td>
                                    {history.performedByEmployeeId 
                                        ? (() => {
                                            const employee = employees.get(history.performedByEmployeeId);
                                            return employee ? `${employee.firstName} ${employee.lastName}` : '-';
                                        })()
                                        : '-'
                                    }
                                </td>
                                <td>{history.reason || '-'}</td>
                            </tr>
                        );
                    })}
                </tbody>
            </Table>
        </div>
    );
};

