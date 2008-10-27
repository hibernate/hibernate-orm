package org.jboss.envers.test.integration.properties;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsProperties extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(PropertiesTestEntity.class);

        cfg.setProperty("org.jboss.envers.versionsTablePrefix", "VP_");
        cfg.setProperty("org.jboss.envers.versionsTableSuffix", "_VS");
        cfg.setProperty("org.jboss.envers.idFieldName", "ver_id");
        cfg.setProperty("org.jboss.envers.revisionFieldName", "ver_rev");
        cfg.setProperty("org.jboss.envers.revisionTypeFieldName", "ver_rev_type");
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        PropertiesTestEntity pte = new PropertiesTestEntity("x");
        em.persist(pte);
        id1 = pte.getId();
        em.getTransaction().commit();

        em.getTransaction().begin();
        pte = em.find(PropertiesTestEntity.class, id1);
        pte.setStr("y");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(PropertiesTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        PropertiesTestEntity ver1 = new PropertiesTestEntity(id1, "x");
        PropertiesTestEntity ver2 = new PropertiesTestEntity(id1, "y");

        assert getVersionsReader().find(PropertiesTestEntity.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(PropertiesTestEntity.class, id1, 2).equals(ver2);
    }
}