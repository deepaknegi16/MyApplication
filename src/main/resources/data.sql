-- Cities (city-center coordinates)
INSERT INTO city (id, name, latitude, longitude) VALUES (1, 'New Delhi', 28.6139, 77.2090);
INSERT INTO city (id, name, latitude, longitude) VALUES (2, 'Mumbai', 19.0760, 72.8777);
INSERT INTO city (id, name, latitude, longitude) VALUES (3, 'Bengaluru', 12.9716, 77.5946);

-- Hotels in New Delhi
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (1, 'The Imperial', 28.6252, 77.2180, 5, false, 1);
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (2, 'Taj Palace', 28.6015, 77.1717, 5, false, 1);
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (3, 'Radisson Blu Dwarka', 28.5823, 77.0500, 4, false, 1);
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (4, 'Hotel Broadway', 28.6415, 77.2400, 3, false, 1);

-- Hotels in Mumbai
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (5, 'Taj Mahal Palace', 18.9220, 72.8332, 5, false, 2);
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (6, 'Trident Nariman Point', 18.9257, 72.8213, 5, false, 2);
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (7, 'ITC Maratha', 19.1075, 72.8672, 5, false, 2);

-- Hotels in Bengaluru (one already soft-deleted)
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (8, 'The Leela Palace', 12.9606, 77.6484, 5, false, 3);
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (9, 'Taj West End', 12.9843, 77.5837, 5, false, 3);
INSERT INTO hotel (id, name, latitude, longitude, rating, deleted, city_id) VALUES (10, 'Old Closed Hotel', 12.9700, 77.5900, 2, true, 3);

-- Keep identity generators ahead of the seeded ids
ALTER TABLE city ALTER COLUMN id RESTART WITH 100;
ALTER TABLE hotel ALTER COLUMN id RESTART WITH 100;
