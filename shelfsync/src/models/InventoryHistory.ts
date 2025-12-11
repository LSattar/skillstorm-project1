import { Item } from './Item';
import { Warehouse } from './Warehouse';
import { Employee } from './Employee';

export class InventoryHistory {
  inventoryHistoryId: number;
  item: Item | null;
  fromWarehouse: Warehouse | null;
  toWarehouse: Warehouse | null;
  quantityChange: number;
  transactionType: string;
  reason: string | null;
  occurredAt: string;
  performedBy: Employee | null;

  constructor(
    inventoryHistoryId: number,
    item: Item | null,
    fromWarehouse: Warehouse | null,
    toWarehouse: Warehouse | null,
    quantityChange: number,
    transactionType: string,
    reason: string | null,
    occurredAt: string,
    performedBy: Employee | null
  ) {
    this.inventoryHistoryId = inventoryHistoryId;
    this.item = item;
    this.fromWarehouse = fromWarehouse;
    this.toWarehouse = toWarehouse;
    this.quantityChange = quantityChange;
    this.transactionType = transactionType;
    this.reason = reason;
    this.occurredAt = occurredAt;
    this.performedBy = performedBy;
  }
}

