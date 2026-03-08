-- =============================================
-- V2__seed_sample_data.sql
-- Sample data for customers and jewelry items
-- =============================================

-- =============================================
-- Sample Customers
-- =============================================
INSERT INTO customers (first_name, last_name, phone, email, address_line1, city, state, pincode) VALUES
    ('Rajesh', 'Sharma', '9876543210', 'rajesh.sharma@email.com', '12 MG Road', 'Mumbai', 'Maharashtra', '400001'),
    ('Priya', 'Patel', '9876543211', 'priya.patel@email.com', '45 Ring Road', 'Ahmedabad', 'Gujarat', '380001'),
    ('Amit', 'Verma', '9876543212', 'amit.verma@email.com', '78 Sector 15', 'Noida', 'Uttar Pradesh', '201301'),
    ('Sunita', 'Reddy', '9876543213', 'sunita.reddy@email.com', '23 Jubilee Hills', 'Hyderabad', 'Telangana', '500033'),
    ('Vikram', 'Singh', '9876543214', 'vikram.singh@email.com', '56 Civil Lines', 'Jaipur', 'Rajasthan', '302001'),
    ('Meena', 'Kumari', '9876543215', 'meena.kumari@email.com', '89 Anna Nagar', 'Chennai', 'Tamil Nadu', '600040'),
    ('Arjun', 'Nair', '9876543216', 'arjun.nair@email.com', '34 MG Road', 'Kochi', 'Kerala', '682016'),
    ('Deepika', 'Joshi', '9876543217', 'deepika.joshi@email.com', '67 Koregaon Park', 'Pune', 'Maharashtra', '411001'),
    ('Rohan', 'Gupta', '9876543218', 'rohan.gupta@email.com', '90 Park Street', 'Kolkata', 'West Bengal', '700016'),
    ('Ananya', 'Desai', '9876543219', 'ananya.desai@email.com', '12 Navrangpura', 'Ahmedabad', 'Gujarat', '380009');

-- =============================================
-- Sample Jewelry Items
-- =============================================

-- Gold Rings (category_id=1, metal_type_id=2 for 22K Gold)
INSERT INTO jewelry_items (sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    ('RNG-G22-001', 'Classic Gold Band', 'Simple 22K gold band ring', 1, 2, 5.200, 5.000, 1500.00, 0.00, '7113', 3, 'IN_STOCK'),
    ('RNG-G22-002', 'Diamond Solitaire Ring', '22K gold ring with single diamond', 1, 2, 4.800, 4.200, 2500.00, 15000.00, '7113', 2, 'IN_STOCK'),
    ('RNG-G22-003', 'Engagement Ring', '22K gold engagement ring with stones', 1, 2, 6.100, 5.500, 3000.00, 8000.00, '7113', 1, 'IN_STOCK'),
    ('RNG-G18-001', 'Rose Gold Ring', '18K rose gold designer ring', 1, 3, 3.800, 3.500, 2000.00, 0.00, '7113', 4, 'IN_STOCK');

-- Gold Necklaces (category_id=2)
INSERT INTO jewelry_items (sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    ('NCK-G22-001', 'Traditional Haar', '22K gold traditional necklace', 2, 2, 35.500, 34.000, 12000.00, 0.00, '7113', 1, 'IN_STOCK'),
    ('NCK-G22-002', 'Choker Necklace', '22K gold choker with temple design', 2, 2, 25.200, 24.000, 9000.00, 5000.00, '7113', 2, 'IN_STOCK'),
    ('NCK-G18-001', 'Diamond Pendant Set', '18K gold necklace with diamond pendant', 2, 3, 18.000, 16.500, 7000.00, 25000.00, '7113', 1, 'IN_STOCK');

-- Gold Earrings (category_id=3)
INSERT INTO jewelry_items (sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    ('EAR-G22-001', 'Jhumka Earrings', '22K gold traditional jhumkas', 3, 2, 12.300, 11.800, 4500.00, 0.00, '7113', 5, 'IN_STOCK'),
    ('EAR-G22-002', 'Stud Earrings', '22K gold diamond studs', 3, 2, 3.200, 2.800, 1800.00, 12000.00, '7113', 3, 'IN_STOCK'),
    ('EAR-G18-001', 'Hoop Earrings', '18K gold hoop earrings', 3, 3, 6.500, 6.200, 2500.00, 0.00, '7113', 4, 'IN_STOCK');

-- Gold Bangles (category_id=4)
INSERT INTO jewelry_items (sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    ('BNG-G22-001', 'Plain Gold Bangle (pair)', '22K gold plain bangles set of 2', 4, 2, 22.000, 21.500, 6000.00, 0.00, '7113', 3, 'IN_STOCK'),
    ('BNG-G22-002', 'Kundan Bangle Set', '22K gold kundan bangles set of 4', 4, 2, 45.000, 42.000, 15000.00, 20000.00, '7113', 1, 'IN_STOCK');

-- Silver Items
INSERT INTO jewelry_items (sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    ('RNG-S92-001', 'Silver Toe Ring', '925 sterling silver toe ring', 1, 4, 2.500, 2.400, 200.00, 0.00, '7113', 10, 'IN_STOCK'),
    ('NCK-S92-001', 'Silver Chain', '925 sterling silver chain 20 inch', 6, 4, 15.000, 14.800, 500.00, 0.00, '7113', 8, 'IN_STOCK'),
    ('ANK-S92-001', 'Silver Anklet (pair)', '925 sterling silver anklets', 7, 4, 20.000, 19.500, 800.00, 0.00, '7113', 6, 'IN_STOCK');

-- Platinum Items
INSERT INTO jewelry_items (sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    ('RNG-P95-001', 'Platinum Wedding Band', '950 platinum wedding band', 1, 6, 8.000, 7.800, 5000.00, 0.00, '7113', 2, 'IN_STOCK'),
    ('PND-P95-001', 'Platinum Diamond Pendant', '950 platinum pendant with diamond', 5, 6, 4.500, 4.000, 3500.00, 30000.00, '7113', 1, 'IN_STOCK');

-- A few sold items for realism
INSERT INTO jewelry_items (sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    ('RNG-G22-004', 'Antique Gold Ring', '22K gold antique finish ring', 1, 2, 7.200, 6.800, 3500.00, 0.00, '7113', 0, 'SOLD'),
    ('NCK-G22-003', 'Bridal Necklace Set', '22K gold heavy bridal necklace', 2, 2, 55.000, 52.000, 18000.00, 35000.00, '7113', 0, 'SOLD');
