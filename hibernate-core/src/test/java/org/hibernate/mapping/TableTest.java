package org.hibernate.mapping;

import junit.framework.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.util.*;
import java.util.Map;

/**
 * @author Damien Hollis
 */
public class TableTest {
    @Test
    public void testGetUniqueKeysWithTwoTheSame() throws SQLException {
        Table testTable = new Table("test");
        testTable.addUniqueKey(createUniqueKey(createColumn("A")));
        testTable.addUniqueKey(createUniqueKey(createColumn("A")));

        Map uniqueKeys = testTable.getUniqueKeys();
        Assert.assertEquals(1, uniqueKeys.size());
    }

    @Test
    public void testGetUniqueKeysWhenSameAsPrimaryKey() throws SQLException {
        Table testTable = new Table("test");
        testTable.setPrimaryKey(createPrimaryKey(createColumn("A")));
        testTable.addUniqueKey(createUniqueKey(createColumn("A")));

        Map uniqueKeys = testTable.getUniqueKeys();
        Assert.assertTrue(uniqueKeys.isEmpty());
    }

    private PrimaryKey createPrimaryKey(Column column) {
        PrimaryKey primaryKey = new PrimaryKey();
        primaryKey.addColumn(column);
        return primaryKey;
    }

    private UniqueKey createUniqueKey(Column column) {
        UniqueKey uniqueKey = new UniqueKey();
        uniqueKey.setName(UUID.randomUUID().toString());
        uniqueKey.addColumn(column);
        return uniqueKey;
    }

    private Column createColumn(String columnName) {
        Column column = new Column();
        column.setName(columnName);
        return column;
    }
}
