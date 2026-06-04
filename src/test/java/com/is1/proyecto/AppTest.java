package com.is1.proyecto;

import com.is1.proyecto.models.User;
import org.javalite.activejdbc.Base;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @BeforeEach
    void setUp() {
        Base.open("org.sqlite.JDBC", "jdbc:sqlite:./target/test.db", "", "");
    }

    @AfterEach
    void tearDown() {
        Base.close();
    }

    @Test
    void testConexionBaseDeDatos() {
        assertTrue(Base.hasConnection(), "Debe existir una conexión activa a la base de datos");
    }

    @Test
    void testHashContrasenia() {
        String password = "miPassword123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        assertNotNull(hash, "El hash no debe ser nulo");
        assertNotEquals(password, hash, "El hash debe ser diferente a la contraseña original");
        assertTrue(BCrypt.checkpw(password, hash), "La verificación del hash debe ser correcta");
    }

    @Test
    void testHashContraseniaIncorrecta() {
        String password = "miPassword123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());

        assertFalse(BCrypt.checkpw("otraPassword", hash), "Una contraseña incorrecta no debe pasar la verificación");
    }
}
