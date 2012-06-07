package org.hibernate.envers.test.integration.superclass.auditparents;

import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

/**
 * Tests mapping of child entity that declares all of its ancestors as audited with {@link Audited#auditParents()} property.
 * All supperclasses are marked with {@link MappedSuperclass} annotation but not {@link Audited}.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class MultipleAuditParentsTest extends BaseEnversJPAFunctionalTestCase {
    private long childMultipleId = 1L;
    private Integer siteMultipleId = null;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(MappedGrandparentEntity.class);
        cfg.addAnnotatedClass(MappedParentEntity.class);
        cfg.addAnnotatedClass(ChildMultipleParentsEntity.class);
        cfg.addAnnotatedClass(StrIntTestEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();
        // Revision 1
        em.getTransaction().begin();
        StrIntTestEntity siteMultiple = new StrIntTestEntity("data 1", 1);
        em.persist(siteMultiple);
        em.persist(new ChildMultipleParentsEntity(childMultipleId, "grandparent 1", "notAudited 1", "parent 1", "child 1", siteMultiple));
        em.getTransaction().commit();
        siteMultipleId = siteMultiple.getId();
    }

    @Test
    public void testCreatedAuditTable() {
        Set<String> expectedColumns = TestTools.makeSet("child", "parent", "relation_id", "grandparent", "id");
        Set<String> unexpectedColumns = TestTools.makeSet("notAudited");

        Table table = getCfg().getClassMapping("org.hibernate.envers.test.integration.superclass.auditparents.ChildMultipleParentsEntity_AUD").getTable();

        for (String columnName : expectedColumns) {
            // Check whether expected column exists.
            Assert.assertNotNull(table.getColumn(new Column(columnName)));
        }
        for (String columnName : unexpectedColumns) {
            // Check whether unexpected column does not exist.
            Assert.assertNull(table.getColumn(new Column(columnName)));
        }
    }

    @Test
    public void testMultipleAuditParents() {
        // expectedMultipleChild.notAudited shall be null, because it is not audited.
        ChildMultipleParentsEntity expectedMultipleChild = new ChildMultipleParentsEntity(childMultipleId, "grandparent 1", null, "parent 1", "child 1", new StrIntTestEntity("data 1", 1, siteMultipleId));
        ChildMultipleParentsEntity child = getAuditReader().find(ChildMultipleParentsEntity.class, childMultipleId, 1);
        Assert.assertEquals(expectedMultipleChild, child);
        Assert.assertEquals(expectedMultipleChild.getRelation().getId(), child.getRelation().getId());
    }
}
