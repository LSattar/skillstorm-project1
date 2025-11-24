CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE category (
    category_id   BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE company (
    company_id     BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    phone          VARCHAR(20),
    email          VARCHAR(255),
    contact_person VARCHAR(150)
);

CREATE TABLE warehouse (
    warehouse_id                BIGSERIAL PRIMARY KEY,
    name                        VARCHAR(100) NOT NULL,
    address                     VARCHAR(200),
    city                        VARCHAR(100),
    state                       CHAR(2),
    zip                         VARCHAR(10),
    manager_employee_id         UUID,
    maximum_capacity_cubic_feet NUMERIC(12,2) NOT NULL
        CHECK (maximum_capacity_cubic_feet >= 0)
);

CREATE TABLE employee (
    employee_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    password_hash        TEXT NOT NULL,
    first_name           VARCHAR(100) NOT NULL,
    last_name            VARCHAR(100) NOT NULL,
    phone                VARCHAR(20),
    email                VARCHAR(255) UNIQUE,
    assigned_warehouse_id BIGINT,
    CONSTRAINT fk_employee_assigned_warehouse
        FOREIGN KEY (assigned_warehouse_id)
        REFERENCES warehouse (warehouse_id)
        ON DELETE SET NULL
);

ALTER TABLE warehouse
    ADD CONSTRAINT fk_warehouse_manager_employee
    FOREIGN KEY (manager_employee_id)
    REFERENCES employee (employee_id)
    ON DELETE SET NULL;

CREATE TABLE item (
    item_id     BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(50) NOT NULL UNIQUE,
    game_title  VARCHAR(150) NOT NULL,
    category_id BIGINT REFERENCES category (category_id),
    company_id  BIGINT REFERENCES company (company_id),
    weight_lbs  NUMERIC(10,2) CHECK (weight_lbs >= 0),
    cubic_feet  NUMERIC(10,2) NOT NULL CHECK (cubic_feet >= 0)
);

CREATE TABLE location (
    location_id  BIGSERIAL PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    aisle        VARCHAR(50),
    rack         VARCHAR(50),
    CONSTRAINT fk_location_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouse (warehouse_id)
);

CREATE TABLE warehouse_item (
    warehouse_id BIGINT NOT NULL,
    item_id      BIGINT NOT NULL,
    quantity     INTEGER NOT NULL CHECK (quantity >= 0),

    CONSTRAINT pk_warehouse_item
        PRIMARY KEY (warehouse_id, item_id),

    CONSTRAINT fk_warehouse_item_warehouse
        FOREIGN KEY (warehouse_id)
        REFERENCES warehouse (warehouse_id),

    CONSTRAINT fk_warehouse_item_item
        FOREIGN KEY (item_id)
        REFERENCES item (item_id)
);

CREATE TABLE inventory_history (
    inventory_history_id     BIGSERIAL PRIMARY KEY,
    item_id                  BIGINT NOT NULL,
    from_warehouse_id        BIGINT,
    to_warehouse_id          BIGINT,
    quantity_change          INTEGER NOT NULL CHECK (quantity_change > 0),
    transaction_type         VARCHAR(20) NOT NULL
        CHECK (transaction_type IN ('RECEIVE', 'SHIP', 'TRANSFER', 'ADJUSTMENT')),
    reason                   TEXT,
    occurred_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    performed_by_employee_id UUID,

    CONSTRAINT fk_inventory_history_item
        FOREIGN KEY (item_id)
        REFERENCES item (item_id),

    CONSTRAINT fk_inventory_history_from_wh
        FOREIGN KEY (from_warehouse_id)
        REFERENCES warehouse (warehouse_id),

    CONSTRAINT fk_inventory_history_to_wh
        FOREIGN KEY (to_warehouse_id)
        REFERENCES warehouse (warehouse_id),

    CONSTRAINT fk_inventory_history_employee
        FOREIGN KEY (performed_by_employee_id)
        REFERENCES employee (employee_id)
);
