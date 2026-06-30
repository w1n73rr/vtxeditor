package org.example.database;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.model.VtxBand;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VtxDaoTest {

    private VtxDao dao;

    @BeforeEach
    void setUp() throws SQLException {
        dao = new SqliteVtxDao();
    }

    @Test
    void testGetAllBands() throws SQLException {
        List<VtxBand> bands = dao.getAllBands();
        assertNotNull(bands);
        assertFalse(bands.isEmpty());

        System.out.println("Тест 3: получение списка сеток из базы данных выполнено успешно");
        System.out.println("Найдено сеток: " + bands.size());
    }
}