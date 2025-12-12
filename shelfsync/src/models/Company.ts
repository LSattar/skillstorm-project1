export class Company {
  companyId: number;
  name: string;
  phone: string | null;
  email: string | null;
  contactPerson: string | null;

  constructor(
    companyId: number,
    name: string,
    phone: string | null,
    email: string | null,
    contactPerson: string | null
  ) {
    this.companyId = companyId;
    this.name = name;
    this.phone = phone;
    this.email = email;
    this.contactPerson = contactPerson;
  }
}

