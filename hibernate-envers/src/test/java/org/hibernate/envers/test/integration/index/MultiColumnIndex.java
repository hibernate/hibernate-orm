package org.hibernate.envers.test.integration.index;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.index.MultiColumnIndexEntity;
import org.hibernate.envers.test.entities.index.SingleColumnIndexEntity;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.testng.annotations.Test;

import java.util.Iterator;

/**
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
public class MultiColumnIndex extends AbstractIndexTest {
    @Test
    public void testIndexCreation() {
        assert multiColumnIndexTable.getIndex("idx1_AUD") != null;
        assert multiColumnIndexTable.getIndex("idx2_AUD") != null;
    }

    @Test(dependsOnMethods = "testIndexCreation")
    public void testNumberOfIndexColumns() {
        assert multiColumnIndexTable.getIndex("idx1_AUD").getColumnSpan() == 2;
        assert multiColumnIndexTable.getIndex("idx2_AUD").getColumnSpan() == 2;
    }

    @Test(dependsOnMethods = "testNumberOfIndexColumns")
    public void testIndexColumnNames() {
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
