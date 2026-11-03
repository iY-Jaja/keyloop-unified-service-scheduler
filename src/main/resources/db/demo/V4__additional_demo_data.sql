INSERT INTO dealership (id, name, timezone)
VALUES ('dealer-2', 'Northside Auto Service', 'Asia/Ho_Chi_Minh');
INSERT INTO dealership (id, name, timezone)
VALUES ('dealer-3', 'Lakeside Vehicle Care', 'Asia/Ho_Chi_Minh');

INSERT INTO customer (id, name, email)
VALUES ('customer-2', 'Riley Chen', 'riley.chen@example.com');
INSERT INTO customer (id, name, email)
VALUES ('customer-3', 'Sam Patel', 'sam.patel@example.com');
INSERT INTO customer (id, name, email)
VALUES ('customer-4', 'Jordan Nguyen', 'jordan.nguyen@example.com');

INSERT INTO vehicle (id, customer_id, vin, make, model, model_year)
VALUES ('vehicle-2', 'customer-2', 'DEMO2234567890123', 'Honda', 'Civic', 2021);
INSERT INTO vehicle (id, customer_id, vin, make, model, model_year)
VALUES ('vehicle-3', 'customer-3', 'DEMO3234567890123', 'Ford', 'Ranger', 2023);
INSERT INTO vehicle (id, customer_id, vin, make, model, model_year)
VALUES ('vehicle-4', 'customer-4', 'DEMO4234567890123', 'Hyundai', 'Tucson', 2020);

INSERT INTO service_type (id, name, duration_minutes)
VALUES ('tire-rotation', 'Tire rotation', 45);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('brake-service', 'Brake service', 120);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('battery-check', 'Battery check', 30);

INSERT INTO service_bay (id, dealership_id, name)
VALUES ('bay-3', 'dealer-2', 'Northside Bay 1');
INSERT INTO service_bay (id, dealership_id, name)
VALUES ('bay-4', 'dealer-2', 'Northside Bay 2');
INSERT INTO service_bay (id, dealership_id, name)
VALUES ('bay-5', 'dealer-3', 'Lakeside Bay 1');

INSERT INTO technician (id, dealership_id, name)
VALUES ('tech-3', 'dealer-2', 'Morgan Reed');
INSERT INTO technician (id, dealership_id, name)
VALUES ('tech-4', 'dealer-2', 'Casey Brooks');
INSERT INTO technician (id, dealership_id, name)
VALUES ('tech-5', 'dealer-3', 'Avery Tran');

INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-4', 'tech-3', 'oil-change');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-5', 'tech-3', 'tire-rotation');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-6', 'tech-4', 'brake-service');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-7', 'tech-4', 'battery-check');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-8', 'tech-5', 'oil-change');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-9', 'tech-5', 'inspection');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-10', 'tech-5', 'tire-rotation');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-11', 'tech-5', 'battery-check');
