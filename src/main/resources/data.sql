-- Carga de roles por defecto
INSERT INTO roles (nombre, sigla) VALUES
    ('Administrador', 'admin')
ON CONFLICT (sigla) DO NOTHING;

INSERT INTO roles (nombre, sigla) VALUES
    ('Cajero', 'cajero')
ON CONFLICT (sigla) DO NOTHING;

INSERT INTO roles (nombre, sigla) VALUES
    ('Invitado', 'invitado')
ON CONFLICT (sigla) DO NOTHING;
