package org.hibernate.envers.test.integration.index;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.index.MultiColumnIndexEntity;
import org.hibernate.envers.test.entities.index.SingleColumnIndexEntity;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
public class SingleColumnIndex extends AbstractIndexTest {
    @Test
    public void testIndexCreation() {
        assert singleColumnIndexTable.getIndex("idx1_AUD") != null;
        assert singleColumnIndexTable.getIndex("idx2_AUD") != null;
        assert singleColumnIndexTable.getIndex("idx3_AUD") != null;
        assert multiColumnIndexTable.getIndex("idx4_AUD") != null;
    }

    @Test(dependsOnMethods = "testIndexCreation")
    public void testNumberOfIndexColumns() {
        assert singleColumnIndexTable.getIndex("idx1_AUD").getColumnSpan() == 1;
        assert singleColumnIndexTable.getIndex("idx2_AUD").getColumnSpan() == 1;
        assert singleColumnIndexTable.getIndex("idx3_AUD").getColumnSpan() == 1;
        assert multiColumnIndexTable.getIndex("idx4_AUD").getColumnSpan() == 1;
    }

    @Test(dependsOnMethods = "testNumberOfIndexColumns")
    public void testIndexColumnNames() {
        assert "id".equals(((Column)singleColumnIndexTable.getIndex("idx1_AUD").getColumnIterator().next()).getName());
        assert "data".equals(((Column)singleColumnIndexTable.getIndex("idx2_AUD").getColumnIterator().next()).getName());
        assert "data".equals(((Column)singleColumnIndexTable.getIndex("idx3_AUD").getColumnIterator().next()).getName());
        assert "parent_id".equals(((Column)multiColumnIndexTable.getIndex("idx4_AUD").getColumnIterator().next()).getName());
    }
}
