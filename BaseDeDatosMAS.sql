CREATE DATABASE servtech;

-- Tabla usuarios
CREATE TABLE usuarios (
id_usuario SERIAL PRIMARY KEY,
usuario VARCHAR(50) UNIQUE NOT NULL,
contrasena_hash VARCHAR(255) NOT NULL,
rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN','SOPORTE','CLIENTE')),
creado_en TIMESTAMP DEFAULT NOW()
);

-- Tabla clientes
CREATE TABLE clientes (
id_cliente SERIAL PRIMARY KEY,
nombre VARCHAR(100) NOT NULL,
email VARCHAR(120),
telefono VARCHAR(30),
creado_en TIMESTAMP DEFAULT NOW()
);

-- Tabla categorias
CREATE TABLE categorias (
id_categoria SERIAL PRIMARY KEY,
nombre VARCHAR(60) NOT NULL
);

-- Tabla tickets
CREATE TABLE tickets (
id SERIAL PRIMARY KEY,
titulo VARCHAR(140) NOT NULL,
descripcion TEXT,
estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','EN_PROCESO','CERRADO')),
prioridad VARCHAR(10) NOT NULL DEFAULT 'MEDIA' CHECK (prioridad IN ('BAJA','MEDIA','ALTA')),
id_cliente INT REFERENCES clientes(id_cliente) ON UPDATE CASCADE ON DELETE RESTRICT,
id_asignado INT REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE SET NULL,
id_categoria INT REFERENCES categorias(id_categoria) ON UPDATE CASCADE ON DELETE SET NULL,
creado_en TIMESTAMP DEFAULT NOW()
);

-- Tabla comentarios de ticket
CREATE TABLE comentarios_ticket (
id_comentario SERIAL PRIMARY KEY,
id_ticket INT NOT NULL REFERENCES tickets(id) ON UPDATE CASCADE ON DELETE CASCADE,
id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT,
comentario TEXT NOT NULL,
creado_en TIMESTAMP DEFAULT NOW()
);

-- Crear tabla tecnicos
CREATE TABLE tecnicos (
  id_tecnico SERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  especialidad VARCHAR(100),
  activo BOOLEAN DEFAULT true,
  creado_en TIMESTAMP DEFAULT NOW()
);

-- Crear tabla usuarios con login
CREATE TABLE usuarios_login (
  id_usuario SERIAL PRIMARY KEY,
  id_tecnico INT REFERENCES tecnicos(id_tecnico) ON DELETE CASCADE,
  usuario VARCHAR(50) UNIQUE NOT NULL,
  contrasena VARCHAR(255) NOT NULL,
  activo BOOLEAN DEFAULT true,
  creado_en TIMESTAMP DEFAULT NOW()
);


SELECT * FROM categorias;

INSERT INTO categorias(nombre) VALUES
  ('General'),
  ('Hardware'),
  ('Software');
  
-- Permitir estado EN_ESPERA
ALTER TABLE tickets DROP CONSTRAINT tickets_estado_check;
ALTER TABLE tickets ADD CONSTRAINT tickets_estado_check 
CHECK (estado IN ('ABIERTO', 'EN_PROCESO', 'CERRADO', 'EN_ESPERA'));

-- Permitir Prioridades en MAYUSCULAS
ALTER TABLE tickets DROP CONSTRAINT tickets_prioridad_check;
ALTER TABLE tickets ADD CONSTRAINT tickets_prioridad_check 
CHECK (prioridad IN ('BAJA', 'MEDIA', 'ALTA'));


SELECT 
    t.id, 
    t.titulo, 
    t.estado, 
    tec.nombre AS tecnico_asignado
FROM tickets t
LEFT JOIN tecnicos tec ON t.id_tecnico = tec.id_tecnico
ORDER BY t.id DESC;



-- Ya que agregamos el metodo para encriptar las contraseñas desde 
--nuestra aplicacion en java, nuestros usuarios antiguos no podran voler
-- a loggearse sin antes haber cambiado o mejor dicho, actualizado sus
-- contraseñas desde aqui (sql) asi que estas "consultas" me ayudaran a
-- borrarlos y volver a registrarlos
-- LIMPIEZA: Borramos los datos viejos
-- Primero usuarios_login porque depende de tecnicos
DELETE FROM usuarios_login;
-- Opcional: Borrar tickets si quieres empezar de cero
DELETE FROM tickets; 
DELETE FROM tecnicos;

-- RESETEAR CONTADORES (Para que los ID empiecen en 1 otra vez)
ALTER SEQUENCE usuarios_login_id_usuario_seq RESTART WITH 1;
ALTER SEQUENCE tecnicos_id_tecnico_seq RESTART WITH 1;
ALTER SEQUENCE tickets_id_seq RESTART WITH 1;

-- 3. INSERTAR TÉCNICOS
INSERT INTO tecnicos (nombre, especialidad) VALUES 
('Raul Admin', 'Sistemas'),   -- ID 1
('Juan Perez', 'Redes'),      -- ID 2
('Maria Lopez', 'Hardware'),  -- ID 3
('Carlos Ruiz', 'Software');  -- ID 4

-- 4. INSERTAR USUARIOS (Todos con contraseña "1234")
-- de momento sacamos las contraseñas hash SHA con 256 bits del 1234 desde internet para poder
-- volver a entrar con esas mismas y no cambiarlas
INSERT INTO usuarios_login (id_tecnico, usuario, contrasena, activo) VALUES 
(1, 'raul',   'e3d78166623f1225b72d50490495147a62058bb9c9354276f830ef4b57797921', true),
(2, 'juan',   '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', true),
(3, 'maria',  '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', true),
(4, 'carlos', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', true);

select * from usuarios_login;
