import { Category } from './Category';
import { Company } from './Company';

export class Item {
  itemId: number;
  sku: string;
  gameTitle: string;
  category: Category | null;
  company: Company | null;
  weightLbs: number;
  cubicFeet: number;

  constructor(
    itemId: number,
    sku: string,
    gameTitle: string,
    category: Category | null,
    company: Company | null,
    weightLbs: number,
    cubicFeet: number
  ) {
    this.itemId = itemId;
    this.sku = sku;
    this.gameTitle = gameTitle;
    this.category = category;
    this.company = company;
    this.weightLbs = weightLbs;
    this.cubicFeet = cubicFeet;
  }
}

