import React, { useEffect, useState } from 'react';
import { Table, Button, ButtonGroup } from 'react-bootstrap';
import { Employee } from '../models/Employee';
import { Warehouse } from '../models/Warehouse';

interface EmployeeTableProps {
    onEdit?: (employeeId: string) => void;
    onDelete?: (employeeId: string, employeeName: string) => void;
}

export const EmployeeTable: React.FC<EmployeeTableProps> = ({ onEdit, onDelete }) => {
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchEmployees = async () => {
            try {
                setLoading(true);
                const response = await fetch('http://localhost:8080/employee');
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                const data = await response.json();
                // Map the DTO response to Employee model
                const mappedEmployees = data.map((dto: any) => {
                    const assignedWarehouse = dto.assignedWarehouse 
                        ? new Warehouse(
                            dto.assignedWarehouse.id,
                            dto.assignedWarehouse.name,
                            dto.assignedWarehouse.address || null,
                            dto.assignedWarehouse.city || null,
                            dto.assignedWarehouse.state || null,
                            dto.assignedWarehouse.zip || null,
                            null, // manager
                            dto.assignedWarehouse.maximumCapacityCubicFeet
                          )
                        : null;
                    
                    return new Employee(
                        dto.id,
                        '', 
                        dto.firstName,
                        dto.lastName,
                        dto.phone,
                        dto.email || null,
                        assignedWarehouse
                    );
                });
                setEmployees(mappedEmployees);
                setError(null);
            } catch (err: any) {
                setError('Failed to load employees: ' + (err.message || 'Unknown error'));
                console.error('Error fetching employees:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchEmployees();
    }, []);

    if (loading) {
        return <div>Loading employees...</div>;
    }

    if (error) {
        return <div className="text-danger">Error: {error}</div>;
    }

    return (
        <Table striped bordered hover>
            <thead>
                <tr>
                    <th>First Name</th>
                    <th>Last Name</th>
                    <th>Phone</th>
                    <th>Email</th>
                    <th>Assigned Warehouse</th>
                    {(onEdit || onDelete) && <th>Actions</th>}
                </tr>
            </thead>
            <tbody>
                {employees.length === 0 ? (
                    <tr>
                        <td colSpan={(onEdit || onDelete) ? 6 : 5} className="text-center">No employees found</td>
                    </tr>
                ) : (
                    employees.map((employee) => (
                        <tr key={employee.employeeId}>
                            <td>{employee.firstName}</td>
                            <td>{employee.lastName}</td>
                            <td>{employee.phone}</td>
                            <td>{employee.email || '-'}</td>
                            <td>{employee.assignedWarehouse?.name || '-'}</td>
                            {(onEdit || onDelete) && (
                                <td>
                                    <ButtonGroup size="sm">
                                        {onEdit && (
                                            <Button 
                                                variant="outline-primary" 
                                                onClick={() => onEdit(employee.employeeId)}
                                                title="Edit employee"
                                            >
                                                <i className="bi bi-pencil"></i>
                                            </Button>
                                        )}
                                        {onDelete && (
                                            <Button 
                                                variant="outline-danger" 
                                                onClick={() => onDelete(employee.employeeId, `${employee.firstName} ${employee.lastName}`)}
                                                title="Delete employee"
                                            >
                                                <i className="bi bi-trash"></i>
                                            </Button>
                                        )}
                                    </ButtonGroup>
                                </td>
                            )}
                        </tr>
                    ))
                )}
            </tbody>
        </Table>
    );
}

