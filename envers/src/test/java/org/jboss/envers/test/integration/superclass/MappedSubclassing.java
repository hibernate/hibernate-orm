package org.jboss.envers.test.integration.superclass;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MappedSubclassing extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SubclassEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        SubclassEntity se1 = new SubclassEntity("x");
        em.persist(se1);
        id1 = se1.getId();
        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();
        se1 = em.find(SubclassEntity.class, id1);
        se1.setStr("y");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SubclassEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        SubclassEntity ver1 = new SubclassEntity(id1, "x");
        SubclassEntity ver2 = new SubclassEntity(id1, "y");

        assert getVersionsReader().find(SubclassEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(SubclassEntity.class, id1, 2).equals(ver2);
    }
}
