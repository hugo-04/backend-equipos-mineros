-- Usuarios que pueden iniciar sesion. La clave se guarda con BCrypt, nunca en texto plano.
CREATE TABLE usuario (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username)
);
