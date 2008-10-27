package org.jboss.envers.test.integration.notinsertable;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class NotInsertable extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(NotInsertableTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        NotInsertableTestEntity dte = new NotInsertableTestEntity("data1");
        em.persist(dte);
        id1 = dte.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        dte = em.find(NotInsertableTestEntity.class, id1);
        dte.setData("data2");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(NotInsertableTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        NotInsertableTestEntity ver1 = new NotInsertableTestEntity(id1, "data1", "data1");
        NotInsertableTestEntity ver2 = new NotInsertableTestEntity(id1, "data2", "data2");

        assert getVersionsReader().find(NotInsertableTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(NotInsertableTestEntity.class, id1, 2).equals(ver2);
    }
}