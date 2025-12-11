import { Employee } from './Employee';

export class Warehouse {
  warehouseId: number;
  name: string;
  address: string | null;
  city: string | null;
  state: string | null;
  zip: string | null;
  manager: Employee | null;
  maximumCapacityCubicFeet: number;

  constructor(
    warehouseId: number,
    name: string,
    address: string | null,
    city: string | null,
    state: string | null,
    zip: string | null,
    manager: Employee | null,
    maximumCapacityCubicFeet: number
  ) {
    this.warehouseId = warehouseId;
    this.name = name;
    this.address = address;
    this.city = city;
    this.state = state;
    this.zip = zip;
    this.manager = manager;
    this.maximumCapacityCubicFeet = maximumCapacityCubicFeet;
  }
}

