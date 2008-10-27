package org.jboss.envers.test.integration.inheritance.single;

import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.jboss.envers.test.AbstractEntityTest;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ParentVersioning extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ChildEntity.class);
        cfg.addAnnotatedClass(ParentEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        // Rev 1
        em.getTransaction().begin();
        ParentEntity pe = new ParentEntity("x");
        em.persist(pe);
        id1 = pe.getId();
        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();
        pe = em.find(ParentEntity.class, id1);
        pe.setData("y");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(ParentEntity.class, id1));
    }

    @Test
    public void testHistoryOfChildId1() {
        assert getVersionsReader().find(ChildEntity.class, id1, 1) == null;
        assert getVersionsReader().find(ChildEntity.class, id1, 2) == null;
    }

    @Test
    public void testHistoryOfParentId1() {
        ParentEntity ver1 = new ParentEntity(id1, "x");
        ParentEntity ver2 = new ParentEntity(id1, "y");

        assert getVersionsReader().find(ParentEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(ParentEntity.class, id1, 2).equals(ver2);
    }

    @Test
    public void testPolymorphicQuery() {
        ParentEntity parentVer1 = new ParentEntity(id1, "x");

        assert getVersionsReader().createQuery().forEntitiesAtRevision(ParentEntity.class, 1).getSingleResult()
                .equals(parentVer1);
        assert getVersionsReader().createQuery().forEntitiesAtRevision(ChildEntity.class, 1)
                .getResultList().size() == 0;
    }
}