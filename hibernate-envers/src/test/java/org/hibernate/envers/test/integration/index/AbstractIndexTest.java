package org.hibernate.envers.test.integration.index;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.index.MultiColumnIndexEntity;
import org.hibernate.envers.test.entities.index.SingleColumnIndexEntity;
import org.hibernate.mapping.Table;
import org.testng.annotations.BeforeClass;

/**
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
public abstract class AbstractIndexTest extends AbstractEntityTest {
    protected Table singleColumnIndexTable = null;
    protected Table multiColumnIndexTable = null;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SingleColumnIndexEntity.class);
        cfg.addAnnotatedClass(MultiColumnIndexEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initLocalVariables() {
        singleColumnIndexTable = getSingleColumnIndexAuditTable();
        multiColumnIndexTable = getMultiColumnIndexAuditTable();
    }

    private Table getSingleColumnIndexAuditTable() {
        return getCfg().getClassMapping("org.hibernate.envers.test.entities.index.SingleColumnIndexEntity_AUD").getTable();
    }

    private Table getMultiColumnIndexAuditTable() {
        return getCfg().getClassMapping("org.hibernate.envers.test.entities.index.MultiColumnIndexEntity_AUD").getTable();
    }
}
