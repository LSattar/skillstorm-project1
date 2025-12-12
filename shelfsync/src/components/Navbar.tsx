import React from 'react';
import { Navbar as BootstrapNavbar, Nav, Container, Form } from 'react-bootstrap';
import { NavLink } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';

export const Navbar = () => {
    const { isDarkMode, toggleTheme } = useTheme();

    return (
        <BootstrapNavbar bg="dark" variant="dark" expand="lg">
            <Container>
                <BootstrapNavbar.Brand as={NavLink} to="/">
                    <i className="bi bi-box-fill me-2"></i>
                    ShelfSync
                </BootstrapNavbar.Brand>
                <BootstrapNavbar.Toggle aria-controls="basic-navbar-nav" />
                <BootstrapNavbar.Collapse id="basic-navbar-nav">
                    <Nav className="me-auto">
                        <Nav.Link as={NavLink} to="/">
                            <i className="bi bi-house-door me-1"></i>
                            Home
                        </Nav.Link>
                        <Nav.Link as={NavLink} to="/warehouses">
                            <i className="bi bi-building me-1"></i>
                            Warehouses
                        </Nav.Link>
                        <Nav.Link as={NavLink} to="/inventory">
                            <i className="bi bi-clipboard-data me-1"></i>
                            Inventory
                        </Nav.Link>
                        <Nav.Link as={NavLink} to="/companies">
                            <i className="bi bi-briefcase me-1"></i>
                            Companies
                        </Nav.Link>
                        <Nav.Link as={NavLink} to="/employees">
                            <i className="bi bi-people me-1"></i>
                            Employees
                        </Nav.Link>
                    </Nav>
                    <Nav>
                        <Form className="ms-2 d-flex align-items-center">
                            <Form.Check
                                type="switch"
                                id="theme-switch"
                                checked={isDarkMode}
                                onChange={toggleTheme}
                                label={isDarkMode ? "Dark" : "Light"}
                                className="text-light"
                            />
                        </Form>
                    </Nav>
                </BootstrapNavbar.Collapse>
            </Container>
        </BootstrapNavbar>
    );
}

