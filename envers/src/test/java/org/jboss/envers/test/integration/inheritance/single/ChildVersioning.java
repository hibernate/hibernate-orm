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
public class ChildVersioning extends AbstractEntityTest {
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
        ChildEntity ce = new ChildEntity("x", 1l);
        em.persist(ce);
        id1 = ce.getId();
        em.getTransaction().commit();

        // Rev 2
        em.getTransaction().begin();
        ce = em.find(ChildEntity.class, id1);
        ce.setData("y");
        ce.setNumber(2l);
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(ChildEntity.class, id1));
    }

    @Test
    public void testHistoryOfChildId1() {
        ChildEntity ver1 = new ChildEntity(id1, "x", 1l);
        ChildEntity ver2 = new ChildEntity(id1, "y", 2l);

        assert getVersionsReader().find(ChildEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(ChildEntity.class, id1, 2).equals(ver2);

        assert getVersionsReader().find(ParentEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(ParentEntity.class, id1, 2).equals(ver2);
    }

    @Test
    public void testPolymorphicQuery() {
        ChildEntity childVer1 = new ChildEntity(id1, "x", 1l);

        assert getVersionsReader().createQuery().forEntitiesAtRevision(ChildEntity.class, 1).getSingleResult()
                .equals(childVer1);

        assert getVersionsReader().createQuery().forEntitiesAtRevision(ParentEntity.class, 1).getSingleResult()
                .equals(childVer1);
    }
}
