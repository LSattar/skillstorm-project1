import { Warehouse } from './Warehouse';

export class Employee {
  employeeId: string;
  passwordHash: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  assignedWarehouse: Warehouse | null;

  constructor(
    employeeId: string,
    passwordHash: string,
    firstName: string,
    lastName: string,
    phone: string,
    email: string | null,
    assignedWarehouse: Warehouse | null
  ) {
    this.employeeId = employeeId;
    this.passwordHash = passwordHash;
    this.firstName = firstName;
    this.lastName = lastName;
    this.phone = phone;
    this.email = email;
    this.assignedWarehouse = assignedWarehouse;
  }
}

