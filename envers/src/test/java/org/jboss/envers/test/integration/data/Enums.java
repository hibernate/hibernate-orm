package org.jboss.envers.test.integration.data;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Enums extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(EnumTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        EnumTestEntity ete = new EnumTestEntity(EnumTestEntity.E1.X, EnumTestEntity.E2.A);
        em.persist(ete);
        id1 = ete.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        ete = em.find(EnumTestEntity.class, id1);
        ete.setEnum1(EnumTestEntity.E1.Y);
        ete.setEnum2(EnumTestEntity.E2.B);
        em.getTransaction().commit();

        newEntityManager();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(EnumTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        EnumTestEntity ver1 = new EnumTestEntity(id1, EnumTestEntity.E1.X, EnumTestEntity.E2.A);
        EnumTestEntity ver2 = new EnumTestEntity(id1, EnumTestEntity.E1.Y, EnumTestEntity.E2.B);

        assert getVersionsReader().find(EnumTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(EnumTestEntity.class, id1, 2).equals(ver2);
    }
}