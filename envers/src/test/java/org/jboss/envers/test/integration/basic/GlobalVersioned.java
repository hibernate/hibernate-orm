package org.jboss.envers.test.integration.basic;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class GlobalVersioned extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity4.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity4 bte1 = new BasicTestEntity4("x", "y");
        em.persist(bte1);
        id1 = bte1.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        bte1 = em.find(BasicTestEntity4.class, id1);
        bte1.setStr1("a");
        bte1.setStr2("b");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(BasicTestEntity4.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        BasicTestEntity4 ver1 = new BasicTestEntity4(id1, "x", "y");
        BasicTestEntity4 ver2 = new BasicTestEntity4(id1, "a", "b");

        assert getVersionsReader().find(BasicTestEntity4.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity4.class, id1, 2).equals(ver2);
    }
}