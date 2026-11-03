INSERT INTO service_type (id, name, duration_minutes)
VALUES ('coolant-flush', 'Coolant flush', 75);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('transmission-service', 'Transmission service', 120);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('wheel-alignment', 'Wheel alignment', 60);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('air-conditioning-service', 'Air conditioning service', 90);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('engine-diagnostic', 'Engine diagnostic', 60);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('windshield-repair', 'Windshield repair', 90);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('spark-plug-replacement', 'Spark plug replacement', 90);
INSERT INTO service_type (id, name, duration_minutes)
VALUES ('safety-inspection', 'Safety inspection', 60);

INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-12', 'tech-3', 'coolant-flush');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-13', 'tech-3', 'transmission-service');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-14', 'tech-3', 'wheel-alignment');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-15', 'tech-3', 'air-conditioning-service');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-16', 'tech-3', 'engine-diagnostic');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-17', 'tech-3', 'windshield-repair');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-18', 'tech-3', 'spark-plug-replacement');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-19', 'tech-3', 'safety-inspection');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-20', 'tech-5', 'coolant-flush');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-21', 'tech-5', 'transmission-service');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-22', 'tech-5', 'wheel-alignment');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-23', 'tech-5', 'air-conditioning-service');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-24', 'tech-5', 'engine-diagnostic');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-25', 'tech-5', 'windshield-repair');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-26', 'tech-5', 'spark-plug-replacement');
INSERT INTO technician_service (id, technician_id, service_type_id)
VALUES ('qual-27', 'tech-5', 'safety-inspection');
