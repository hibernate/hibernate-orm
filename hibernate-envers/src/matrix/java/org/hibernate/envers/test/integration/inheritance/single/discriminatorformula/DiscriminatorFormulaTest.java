package org.hibernate.envers.test.integration.inheritance.single.discriminatorformula;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class DiscriminatorFormulaTest extends AbstractEntityTest {
    private PersistentClass parentAudit = null;
    private ChildEntity childVer1 = null;
    private ChildEntity childVer2 = null;
    private ParentEntity parentVer1 = null;
    private ParentEntity parentVer2 = null;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ClassTypeEntity.class);
        cfg.addAnnotatedClass(ParentEntity.class);
        cfg.addAnnotatedClass(ChildEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        parentAudit = getCfg().getClassMapping("org.hibernate.envers.test.integration.inheritance.single.discriminatorformula.ParentEntity_AUD");

        EntityManager em = getEntityManager();

        // Child entity type
        em.getTransaction().begin();
        ClassTypeEntity childType = new ClassTypeEntity();
        childType.setType(ClassTypeEntity.CHILD_TYPE);
        em.persist(childType);
        Long childTypeId = childType.getId();
        em.getTransaction().commit();

        // Parent entity type
        em.getTransaction().begin();
        ClassTypeEntity parentType = new ClassTypeEntity();
        parentType.setType(ClassTypeEntity.PARENT_TYPE);
        em.persist(parentType);
        Long parentTypeId = parentType.getId();
        em.getTransaction().commit();

        // Child Rev 1
        em.getTransaction().begin();
        ChildEntity child = new ChildEntity(childTypeId, "Child data", "Child specific data");
        em.persist(child);
        Long childId = child.getId();
        em.getTransaction().commit();

        // Parent Rev 2
        em.getTransaction().begin();
        ParentEntity parent = new ParentEntity(parentTypeId, "Parent data");
        em.persist(parent);
        Long parentId = parent.getId();
        em.getTransaction().commit();

        // Child Rev 3
        em.getTransaction().begin();
        child = em.find(ChildEntity.class, childId);
        child.setData("Child data modified");
        em.getTransaction().commit();

        // Parent Rev 4
        em.getTransaction().begin();
        parent = em.find(ParentEntity.class, parentId);
        parent.setData("Parent data modified");
        em.getTransaction().commit();

        childVer1 = new ChildEntity(childId, childTypeId, "Child data", "Child specific data");
        childVer2 = new ChildEntity(childId, childTypeId, "Child data modified", "Child specific data");
        parentVer1 = new ParentEntity(parentId, parentTypeId, "Parent data");
        parentVer2 = new ParentEntity(parentId, parentTypeId, "Parent data modified");
    }

    @Test
    public void testDiscriminatorFormulaInAuditTable() {
        assert parentAudit.getDiscriminator().hasFormula();
        Iterator iterator = parentAudit.getDiscriminator().getColumnIterator();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o instanceof Formula) {
                Formula formula = (Formula) o;
                assert formula.getText().equals(ParentEntity.DISCRIMINATOR_QUERY);
                return;
            }
        }
        assert false;
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 3).equals(getAuditReader().getRevisions(ChildEntity.class, childVer1.getId()));
        assert Arrays.asList(2, 4).equals(getAuditReader().getRevisions(ParentEntity.class, parentVer1.getId()));
    }

    @Test
    public void testHistoryOfParent() {
        assert getAuditReader().find(ParentEntity.class, parentVer1.getId(), 2).equals(parentVer1);
        assert getAuditReader().find(ParentEntity.class, parentVer2.getId(), 4).equals(parentVer2);
    }

    @Test
    public void testHistoryOfChild() {
        assert getAuditReader().find(ChildEntity.class, childVer1.getId(), 1).equals(childVer1);
        assert getAuditReader().find(ChildEntity.class, childVer2.getId(), 3).equals(childVer2);
    }

    @Test
    public void testPolymorphicQuery() {
        assert getAuditReader().createQuery().forEntitiesAtRevision(ChildEntity.class, 1).getSingleResult().equals(childVer1);
        assert getAuditReader().createQuery().forEntitiesAtRevision(ParentEntity.class, 1).getSingleResult().equals(childVer1);

        List childEntityRevisions = getAuditReader().createQuery().forRevisionsOfEntity(ChildEntity.class, true, false).getResultList();
        assert Arrays.asList(childVer1, childVer2).equals(childEntityRevisions);

        List parentEntityRevisions = getAuditReader().createQuery().forRevisionsOfEntity(ParentEntity.class, true, false).getResultList();
        assert Arrays.asList(childVer1, parentVer1, childVer2, parentVer2).equals(parentEntityRevisions);
    }
}
