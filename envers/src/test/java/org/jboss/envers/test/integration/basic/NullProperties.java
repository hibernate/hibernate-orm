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
public class NullProperties extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity1.class);
    }

    private Integer addNewEntity(String str, long lng) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity1 bte1 = new BasicTestEntity1(str, lng);
        em.persist(bte1);
        em.getTransaction().commit();

        return bte1.getId();
    }

    private void modifyEntity(Integer id, String str, long lng) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity1 bte1 = em.find(BasicTestEntity1.class, id);
        bte1.setLong1(lng);
        bte1.setStr1(str);
        em.getTransaction().commit();
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id1 = addNewEntity("x", 1); // rev 1
        id2 = addNewEntity(null, 20); // rev 2

        modifyEntity(id1, null, 1); // rev 3
        modifyEntity(id2, "y2", 20); // rev 4
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 3).equals(getVersionsReader().getRevisions(BasicTestEntity1.class, id1));

        assert Arrays.asList(2, 4).equals(getVersionsReader().getRevisions(BasicTestEntity1.class, id2));
    }

    @Test
    public void testHistoryOfId1() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id1, "x", 1);
        BasicTestEntity1 ver2 = new BasicTestEntity1(id1, null, 1);

        assert getVersionsReader().find(BasicTestEntity1.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 2).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 3).equals(ver2);
        assert getVersionsReader().find(BasicTestEntity1.class, id1, 4).equals(ver2);
    }

    @Test
    public void testHistoryOfId2() {
        BasicTestEntity1 ver1 = new BasicTestEntity1(id2, null, 20);
        BasicTestEntity1 ver2 = new BasicTestEntity1(id2, "y2", 20);

        assert getVersionsReader().find(BasicTestEntity1.class, id2, 1) == null;
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 2).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 3).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity1.class, id2, 4).equals(ver2);
    }
}