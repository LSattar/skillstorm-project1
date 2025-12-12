import React, { useEffect, useState } from 'react';
import { Table, Button, ButtonGroup } from 'react-bootstrap';
import { Company } from '../models/Company';

interface CompanyTableProps {
    onEdit?: (companyId: number) => void;
    onDelete?: (companyId: number, companyName: string) => void;
}

export const CompanyTable: React.FC<CompanyTableProps> = ({ onEdit, onDelete }) => {
    const [companies, setCompanies] = useState<Company[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchCompanies = async () => {
            try {
                setLoading(true);
                const response = await fetch('http://localhost:8080/company');
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                const data = await response.json();
                const mappedCompanies = data.map((dto: any) => 
                    new Company(
                        dto.id,
                        dto.name,
                        dto.phone || null,
                        dto.email || null,
                        dto.contactPerson || null
                    )
                );
                setCompanies(mappedCompanies);
                setError(null);
            } catch (err: any) {
                setError('Failed to load companies: ' + (err.message || 'Unknown error'));
                console.error('Error fetching companies:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchCompanies();
    }, []);

    if (loading) {
        return <div>Loading companies...</div>;
    }

    if (error) {
        return <div className="text-danger">Error: {error}</div>;
    }

    return (
        <Table striped bordered hover>
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Phone</th>
                    <th>Email</th>
                    <th>Contact Person</th>
                    {(onEdit || onDelete) && <th>Actions</th>}
                </tr>
            </thead>
            <tbody>
                {companies.length === 0 ? (
                    <tr>
                        <td colSpan={(onEdit || onDelete) ? 5 : 4} className="text-center">No companies found</td>
                    </tr>
                ) : (
                    companies.map((company) => (
                        <tr key={company.companyId}>
                            <td>{company.name}</td>
                            <td>{company.phone || '-'}</td>
                            <td>{company.email || '-'}</td>
                            <td>{company.contactPerson || '-'}</td>
                            {(onEdit || onDelete) && (
                                <td>
                                    <ButtonGroup size="sm">
                                        {onEdit && (
                                            <Button 
                                                variant="outline-primary" 
                                                onClick={() => onEdit(company.companyId)}
                                                title="Edit company"
                                            >
                                                <i className="bi bi-pencil"></i>
                                            </Button>
                                        )}
                                        {onDelete && (
                                            <Button 
                                                variant="outline-danger" 
                                                onClick={() => onDelete(company.companyId, company.name)}
                                                title="Delete company"
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