package org.jboss.envers.test.integration.basic;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.exception.NotVersionedException;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotVersioned extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity3.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity3 bte1 = new BasicTestEntity3("x", "y");
        em.persist(bte1);
        id1 = bte1.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        bte1 = em.find(BasicTestEntity3.class, id1);
        bte1.setStr1("a");
        bte1.setStr2("b");
        em.getTransaction().commit();
    }

    @Test(expectedExceptions = NotVersionedException.class)
    public void testRevisionsCounts() {
        getVersionsReader().getRevisions(BasicTestEntity3.class, id1);
    }

    @Test(expectedExceptions = NotVersionedException.class)
    public void testHistoryOfId1() {
        getVersionsReader().find(BasicTestEntity3.class, id1, 1);
    }
}