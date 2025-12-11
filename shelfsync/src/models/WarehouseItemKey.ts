export class WarehouseItemKey {
  warehouseId: number;
  itemId: number;

  constructor(
    warehouseId: number,
    itemId: number
  ) {
    this.warehouseId = warehouseId;
    this.itemId = itemId;
  }
}

