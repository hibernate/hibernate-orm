package org.hibernate.envers.test.integration.superclass.auditparents;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrIntTestEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Set;

/**
 * Tests several configurations of entity hierarchy that utilizes {@link Audited#auditParents()} property.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AuditParentsTest extends AbstractEntityTest {
    private long childSingleId = 1L;
    private long childMultipleId = 2L;
    private long childImpTransId = 3L;
    private long childExpTransId = 4L;
    private long babyCompleteId = 5L;
    private Integer siteSingleId = null;
    private Integer siteMultipleId = null;
    private Integer siteCompleteId = null;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(MappedGrandparentEntity.class);
        cfg.addAnnotatedClass(MappedParentEntity.class);
        cfg.addAnnotatedClass(ChildSingleParentEntity.class);
        cfg.addAnnotatedClass(ChildMultipleParentsEntity.class);
        cfg.addAnnotatedClass(TransitiveParentEntity.class);
        cfg.addAnnotatedClass(ImplicitTransitiveChildEntity.class);
        cfg.addAnnotatedClass(ExplicitTransitiveChildEntity.class);
        cfg.addAnnotatedClass(StrIntTestEntity.class);
        cfg.addAnnotatedClass(ChildCompleteEntity.class);
        cfg.addAnnotatedClass(BabyCompleteEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();
        
        // Revision 1
        em.getTransaction().begin();
        StrIntTestEntity siteSingle = new StrIntTestEntity("data 1", 1);
        em.persist(siteSingle);
        em.persist(new ChildSingleParentEntity(childSingleId, "grandparent 1", "notAudited 1", "parent 1", "child 1", siteSingle));
        em.getTransaction().commit();
        siteSingleId = siteSingle.getId();

        // Revision 2
        em.getTransaction().begin();
        StrIntTestEntity siteMultiple = new StrIntTestEntity("data 2", 2);
        em.persist(siteMultiple);
        em.persist(new ChildMultipleParentsEntity(childMultipleId, "grandparent 2", "notAudited 2", "parent 2", "child 2", siteMultiple));
        em.getTransaction().commit();
        siteMultipleId = siteMultiple.getId();

        // Revision 3
        em.getTransaction().begin();
        em.persist(new ImplicitTransitiveChildEntity(childImpTransId, "grandparent 3", "notAudited 3", "parent 3", "child 3"));
        em.getTransaction().commit();

        // Revision 4
        em.getTransaction().begin();
        em.persist(new ExplicitTransitiveChildEntity(childExpTransId, "grandparent 4", "notAudited 4", "parent 4", "child 4"));
        em.getTransaction().commit();

        // Revision 5
        em.getTransaction().begin();
        StrIntTestEntity siteComplete = new StrIntTestEntity("data 5", 5);
        em.persist(siteComplete);
        em.persist(new BabyCompleteEntity(babyCompleteId, "grandparent 5", "notAudited 5", "parent 5", "child 5", siteComplete, "baby 5"));
        em.getTransaction().commit();
        siteCompleteId = siteComplete.getId();
    }

    @Test
    public void testCreatedAuditTables() {
        Table babyCompleteTable = getCfg().getClassMapping("org.hibernate.envers.test.integration.superclass.auditparents.BabyCompleteEntity_AUD").getTable();
        checkTableColumns(TestTools.makeSet("baby", "child", "parent", "relation_id", "grandparent", "id"), babyCompleteTable);

        Table explicitTransChildTable = getCfg().getClassMapping("org.hibernate.envers.test.integration.superclass.auditparents.ExplicitTransitiveChildEntity_AUD").getTable();
        checkTableColumns(TestTools.makeSet("child", "parent", "grandparent", "id"), explicitTransChildTable);

        Table implicitTransChildTable = getCfg().getClassMapping("org.hibernate.envers.test.integration.superclass.auditparents.ImplicitTransitiveChildEntity_AUD").getTable();
        checkTableColumns(TestTools.makeSet("child", "parent", "grandparent", "id"), implicitTransChildTable);

        Table multipleParentChildTable = getCfg().getClassMapping("org.hibernate.envers.test.integration.superclass.auditparents.ChildMultipleParentsEntity_AUD").getTable();
        checkTableColumns(TestTools.makeSet("child", "parent", "relation_id", "grandparent", "id"), multipleParentChildTable);

        Table singleParentChildTable = getCfg().getClassMapping("org.hibernate.envers.test.integration.superclass.auditparents.ChildSingleParentEntity_AUD").getTable();
        checkTableColumns(TestTools.makeSet("child", "grandparent", "id"), singleParentChildTable);
    }

    @SuppressWarnings("unchecked")
    private void checkTableColumns(Set<String> expectedColumns, Table table) {
        for (String columnName : expectedColumns) {
            // Check whether expected column exists.
            Assert.assertNotNull(table.getColumn(new Column(columnName)));
        }
    }

    @Test
    public void testSingleAuditParent() {
        // expectedSingleChild.parent, expectedSingleChild.relation and expectedSingleChild.notAudited shall be null, because they are not audited.
        ChildSingleParentEntity expectedSingleChild = new ChildSingleParentEntity(childSingleId, "grandparent 1", null, null, "child 1", null);
        ChildSingleParentEntity child = getAuditReader().find(ChildSingleParentEntity.class, childSingleId, 1);
        Assert.assertEquals(expectedSingleChild, child);
        Assert.assertNull(child.getRelation());
    }

    @Test
    public void testMultipleAuditParents() {
        // expectedMultipleChild.notAudited shall be null, because it is not audited.
        ChildMultipleParentsEntity expectedMultipleChild = new ChildMultipleParentsEntity(childMultipleId, "grandparent 2", null, "parent 2", "child 2", new StrIntTestEntity("data 2", 2, siteMultipleId));
        ChildMultipleParentsEntity child = getAuditReader().find(ChildMultipleParentsEntity.class, childMultipleId, 2);
        Assert.assertEquals(expectedMultipleChild, child);
        Assert.assertEquals(expectedMultipleChild.getRelation().getId(), child.getRelation().getId());
    }

    @Test
    public void testImplicitTransitiveAuditParents() {
        // expectedChild.notAudited shall be null, because it is not audited.
        ImplicitTransitiveChildEntity expectedChild = new ImplicitTransitiveChildEntity(childImpTransId, "grandparent 3", null, "parent 3", "child 3");
        ImplicitTransitiveChildEntity child = getAuditReader().find(ImplicitTransitiveChildEntity.class, childImpTransId, 3);
        Assert.assertEquals(expectedChild, child);
    }

    @Test
    public void testExplicitTransitiveAuditParents() {
        // expectedChild.notAudited shall be null, because it is not audited.
        ExplicitTransitiveChildEntity expectedChild = new ExplicitTransitiveChildEntity(childExpTransId, "grandparent 4", null, "parent 4", "child 4");
        ExplicitTransitiveChildEntity child = getAuditReader().find(ExplicitTransitiveChildEntity.class, childExpTransId, 4);
        Assert.assertEquals(expectedChild, child);
    }

    @Test
    public void testCompleteAuditParents() {
        // expectedBaby.notAudited shall be null, because it is not audited.
        BabyCompleteEntity expectedBaby = new BabyCompleteEntity(babyCompleteId, "grandparent 5", null, "parent 5", "child 5", new StrIntTestEntity("data 5", 5, siteCompleteId), "baby 5");
        BabyCompleteEntity baby = getAuditReader().find(BabyCompleteEntity.class, babyCompleteId, 5);
        Assert.assertEquals(expectedBaby, baby);
        Assert.assertEquals(expectedBaby.getRelation().getId(), baby.getRelation().getId());
    }
}
