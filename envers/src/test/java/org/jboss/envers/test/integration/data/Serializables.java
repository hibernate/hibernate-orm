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
public class Serializables extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SerializableTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        SerializableTestEntity ste = new SerializableTestEntity(new SerObject("d1"));
        em.persist(ste);
        id1 = ste.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        ste = em.find(SerializableTestEntity.class, id1);
        ste.setObj(new SerObject("d2"));
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SerializableTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        SerializableTestEntity ver1 = new SerializableTestEntity(id1, new SerObject("d1"));
        SerializableTestEntity ver2 = new SerializableTestEntity(id1, new SerObject("d2"));

        assert getVersionsReader().find(SerializableTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(SerializableTestEntity.class, id1, 2).equals(ver2);
    }
}