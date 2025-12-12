import { WarehouseItemKey } from './WarehouseItemKey';
import { Warehouse } from './Warehouse';
import { Item } from './Item';

export class WarehouseItem {
  id: WarehouseItemKey;
  warehouse: Warehouse | null;
  item: Item | null;
  quantity: number;

  constructor(
    id: WarehouseItemKey,
    warehouse: Warehouse | null,
    item: Item | null,
    quantity: number
  ) {
    this.id = id;
    this.warehouse = warehouse;
    this.item = item;
    this.quantity = quantity;
  }
}

