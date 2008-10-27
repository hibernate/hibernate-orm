package org.jboss.envers.test.integration.basic;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.IntTestEntity;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Simple extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        IntTestEntity ite = new IntTestEntity(10);
        em.persist(ite);
        id1 = ite.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        ite = em.find(IntTestEntity.class, id1);
        ite.setNumber(20);
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(IntTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        IntTestEntity ver1 = new IntTestEntity(10, id1);
        IntTestEntity ver2 = new IntTestEntity(20, id1);

        assert getVersionsReader().find(IntTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(IntTestEntity.class, id1, 2).equals(ver2);
    }
}