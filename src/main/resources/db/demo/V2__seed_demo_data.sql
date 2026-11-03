INSERT INTO dealership (id, name, timezone)
VALUES ('dealer-1', 'Keyloop Demo Motors', 'Asia/Ho_Chi_Minh');
INSERT INTO customer (id, name, email)
VALUES ('customer-1', 'Alex Morgan', 'alex@example.com');
INSERT INTO vehicle (id, customer_id, vin, make, model, model_year)
VALUES ('vehicle-1', 'customer-1', 'DEMO1234567890123', 'Toyota', 'Corolla', 2022);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('oil-change', 'Oil change', 60);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('inspection', 'Multi-point inspection', 90);
INSERT INTO service_bay (id, dealership_id, name)
VALUES ('bay-1', 'dealer-1', 'Bay 1');
INSERT INTO service_bay (id, dealership_id, name)
VALUES ('bay-2', 'dealer-1', 'Bay 2');
INSERT INTO technician (id, dealership_id, name)
VALUES ('tech-1', 'dealer-1', 'Jamie Lee');
INSERT INTO technician (id, dealership_id, name)
VALUES ('tech-2', 'dealer-1', 'Taylor Kim');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-1', 'tech-1', 'oil-change');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-2', 'tech-1', 'inspection');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-3', 'tech-2', 'inspection');
