package org.hibernate.envers.test.integration.components;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.components.UniquePropsEntity;
import org.hibernate.envers.test.entities.components.UniquePropsNotAuditedEntity;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6636")
public class PropertiesGroupTest extends AbstractSessionTest {
    private PersistentClass uniquePropsAudit = null;
    private PersistentClass uniquePropsNotAuditedAudit = null;
    private UniquePropsEntity entityRev1 = null;
    private UniquePropsNotAuditedEntity entityNotAuditedRev2 = null;

    protected void initMappings() throws MappingException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/components/UniquePropsEntity.hbm.xml");
        config.addFile(new File(url.toURI()));
        url = Thread.currentThread().getContextClassLoader().getResource("mappings/components/UniquePropsNotAuditedEntity.hbm.xml");
        config.addFile(new File(url.toURI()));
    }

    @Test
    @Priority(10)
    public void initData() {
        uniquePropsAudit = getCfg().getClassMapping("org.hibernate.envers.test.entities.components.UniquePropsEntity_AUD");
        uniquePropsNotAuditedAudit = getCfg().getClassMapping("org.hibernate.envers.test.entities.components.UniquePropsNotAuditedEntity_AUD");

        // Revision 1
        getSession().getTransaction().begin();
        UniquePropsEntity ent = new UniquePropsEntity();
        ent.setData1("data1");
        ent.setData2("data2");
        getSession().persist(ent);
        getSession().getTransaction().commit();

        entityRev1 = new UniquePropsEntity(ent.getId(), ent.getData1(), ent.getData2());

        // Revision 2
        getSession().getTransaction().begin();
        UniquePropsNotAuditedEntity entNotAud = new UniquePropsNotAuditedEntity();
        entNotAud.setData1("data3");
        entNotAud.setData2("data4");
        getSession().persist(entNotAud);
        getSession().getTransaction().commit();

        entityNotAuditedRev2 = new UniquePropsNotAuditedEntity(entNotAud.getId(), entNotAud.getData1(), null);
    }

    @Test
    public void testAuditTableColumns() {
        Assert.assertNotNull(uniquePropsAudit.getTable().getColumn(new Column("DATA1")));
        Assert.assertNotNull(uniquePropsAudit.getTable().getColumn(new Column("DATA2")));

        Assert.assertNotNull(uniquePropsNotAuditedAudit.getTable().getColumn(new Column("DATA1")));
        Assert.assertNull(uniquePropsNotAuditedAudit.getTable().getColumn(new Column("DATA2")));
    }

    @Test
    public void testHistoryOfUniquePropsEntity() {
        Assert.assertEquals(entityRev1, getAuditReader().find(UniquePropsEntity.class, entityRev1.getId(), 1));
    }

    @Test
    public void testHistoryOfUniquePropsNotAuditedEntity() {
        Assert.assertEquals(entityNotAuditedRev2, getAuditReader().find(UniquePropsNotAuditedEntity.class, entityNotAuditedRev2.getId(), 2));
    }
}
