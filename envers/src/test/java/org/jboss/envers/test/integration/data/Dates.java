package org.jboss.envers.test.integration.data;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Dates extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(DateTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        DateTestEntity dte = new DateTestEntity(new Date(12345000));
        em.persist(dte);
        id1 = dte.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        dte = em.find(DateTestEntity.class, id1);
        dte.setDate(new Date(45678000));
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(DateTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        DateTestEntity ver1 = new DateTestEntity(id1, new Date(12345000));
        DateTestEntity ver2 = new DateTestEntity(id1, new Date(45678000));

        assert getVersionsReader().find(DateTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(DateTestEntity.class, id1, 2).equals(ver2);
    }
}