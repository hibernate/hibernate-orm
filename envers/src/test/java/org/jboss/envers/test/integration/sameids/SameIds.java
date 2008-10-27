package org.jboss.envers.test.integration.sameids;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * A test which checks that if we add two different entities with the same ids in one revision, they
 * will both be stored.
 * @author Adam Warski (adam at warski dot org)
 */
public class SameIds extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SameIdTestEntity1.class);
        cfg.addAnnotatedClass(SameIdTestEntity2.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        SameIdTestEntity1 site1 = new SameIdTestEntity1(1, "str1");
        SameIdTestEntity2 site2 = new SameIdTestEntity2(1, "str1");

        em.persist(site1);
        em.persist(site2);
        em.getTransaction().commit();

        em.getTransaction().begin();
        site1 = em.find(SameIdTestEntity1.class, 1);
        site2 = em.find(SameIdTestEntity2.class, 1);
        site1.setStr1("str2");
        site2.setStr1("str2");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SameIdTestEntity1.class, 1));
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SameIdTestEntity2.class, 1));
    }

    @Test
    public void testHistoryOfSite1() {
        SameIdTestEntity1 ver1 = new SameIdTestEntity1(1, "str1");
        SameIdTestEntity1 ver2 = new SameIdTestEntity1(1, "str2");

        assert getVersionsReader().find(SameIdTestEntity1.class, 1, 1).equals(ver1);
        assert getVersionsReader().find(SameIdTestEntity1.class, 1, 2).equals(ver2);
    }

    @Test
    public void testHistoryOfSite2() {
        SameIdTestEntity2 ver1 = new SameIdTestEntity2(1, "str1");
        SameIdTestEntity2 ver2 = new SameIdTestEntity2(1, "str2");

        assert getVersionsReader().find(SameIdTestEntity2.class, 1, 1).equals(ver1);
        assert getVersionsReader().find(SameIdTestEntity2.class, 1, 2).equals(ver2);
    }
}
