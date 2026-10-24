-- Dati di esempio per sviluppo e prove.
-- UUID fissi: così frontend, test e chiamate di prova possono riferirsi sempre agli stessi prodotti.

INSERT INTO products (id, sku, name, description, price, stock) VALUES
    ('11111111-1111-1111-1111-000000000001', 'KB-001',  'Tastiera meccanica',     'Tastiera meccanica layout italiano, switch brown',   89.90, 25),
    ('11111111-1111-1111-1111-000000000002', 'MS-001',  'Mouse wireless',         'Mouse ergonomico wireless, 6 tasti',                 34.50, 40),
    ('11111111-1111-1111-1111-000000000003', 'MN-001',  'Monitor 27" 4K',         'Monitor IPS 27 pollici, risoluzione 3840x2160',     329.00, 10),
    ('11111111-1111-1111-1111-000000000004', 'HS-001',  'Cuffie con microfono',   'Cuffie over-ear con microfono a cancellazione',      59.99, 15),
    ('11111111-1111-1111-1111-000000000005', 'WC-001',  'Webcam Full HD',         'Webcam 1080p con autofocus',                         45.00,  8),
    ('11111111-1111-1111-1111-000000000006', 'DK-001',  'Docking station USB-C',  'Dock USB-C con 2 HDMI, Ethernet e ricarica 100W',   129.00,  5),
    ('11111111-1111-1111-1111-000000000007', 'CB-001',  'Cavo HDMI 2m',           'Cavo HDMI 2.1, 2 metri',                              9.90, 100),
    ('11111111-1111-1111-1111-000000000008', 'LP-001',  'Supporto per laptop',    'Supporto in alluminio regolabile',                   27.00,  0);  -- esaurito: utile per provare un ordine rifiutato
