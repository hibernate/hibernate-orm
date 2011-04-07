package org.hibernate.envers.test.integration.index;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.index.MultiColumnIndexEntity;
import org.hibernate.envers.test.entities.index.SingleColumnIndexEntity;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Iterator;

/**
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
public class DatabaseIndexTest extends AbstractEntityTest {
    private Table singleColumnIndexTable = null;
    private Table multiColumnIndexTable = null;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SingleColumnIndexEntity.class);
        cfg.addAnnotatedClass(MultiColumnIndexEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initLocalVariables() {
        singleColumnIndexTable = getCfg().getClassMapping("org.hibernate.envers.test.entities.index.SingleColumnIndexEntity_AUD").getTable();
        multiColumnIndexTable = getCfg().getClassMapping("org.hibernate.envers.test.entities.index.MultiColumnIndexEntity_AUD").getTable();
    }

    @Test
    public void testCreationOfSingleColumnIndex() {
        assert singleColumnIndexTable.getIndex("idx1_AUD") != null;
        assert singleColumnIndexTable.getIndex("idx2_AUD") != null;
        assert singleColumnIndexTable.getIndex("idx3_AUD") != null;
        assert multiColumnIndexTable.getIndex("idx4_AUD") != null;
    }

    @Test(dependsOnMethods = "testCreationOfSingleColumnIndex")
    public void testColumnSpanOfSingleColumnIndex() {
        assert singleColumnIndexTable.getIndex("idx1_AUD").getColumnSpan() == 1;
        assert singleColumnIndexTable.getIndex("idx2_AUD").getColumnSpan() == 1;
        assert singleColumnIndexTable.getIndex("idx3_AUD").getColumnSpan() == 1;
        assert multiColumnIndexTable.getIndex("idx4_AUD").getColumnSpan() == 1;
    }

    @Test(dependsOnMethods = "testColumnSpanOfSingleColumnIndex")
    public void testColumnNamesOfSingleColumnIndex() {
        assert "id".equals(((Column)singleColumnIndexTable.getIndex("idx1_AUD").getColumnIterator().next()).getName());
        assert "data".equals(((Column)singleColumnIndexTable.getIndex("idx2_AUD").getColumnIterator().next()).getName());
        assert "data".equals(((Column)singleColumnIndexTable.getIndex("idx3_AUD").getColumnIterator().next()).getName());
        assert "parent_id".equals(((Column)multiColumnIndexTable.getIndex("idx4_AUD").getColumnIterator().next()).getName());
    }

    @Test
    public void testCreationOfMultiColumnIndex() {
        assert multiColumnIndexTable.getIndex("idx1_AUD") != null;
        assert multiColumnIndexTable.getIndex("idx2_AUD") != null;
    }

    @Test(dependsOnMethods = "testCreationOfMultiColumnIndex")
    public void testColumnSpanOfMultiColumnIndex() {
        assert multiColumnIndexTable.getIndex("idx1_AUD").getColumnSpan() == 2;
        assert multiColumnIndexTable.getIndex("idx2_AUD").getColumnSpan() == 2;
    }

    @Test(dependsOnMethods = "testColumnSpanOfMultiColumnIndex")
    public void testColumnNamesOfMultiColumnIndex() {
        assert multiColumnIndexTable.getIndex("idx1_AUD").containsColumn(getTableColumnByName(multiColumnIndexTable, "id"));
        assert multiColumnIndexTable.getIndex("idx1_AUD").containsColumn(getTableColumnByName(multiColumnIndexTable, "data1"));
        assert multiColumnIndexTable.getIndex("idx2_AUD").containsColumn(getTableColumnByName(multiColumnIndexTable, "data1"));
        assert multiColumnIndexTable.getIndex("idx2_AUD").containsColumn(getTableColumnByName(multiColumnIndexTable, "data2"));
    }

    private Column getTableColumnByName(Table table, String columnName) {
        Iterator<Column> columnIterator = table.getColumnIterator();
        while (columnIterator.hasNext()) {
            Column column = columnIterator.next();
            if (columnName.equals(column.getName())) {
                return column;
            }
        }
        throw new RuntimeException("Column '" + columnName + "' has not been found in table '" + table.getName() + "'.");
    }
}
