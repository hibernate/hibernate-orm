package org.jboss.envers.test.integration.naming;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicNaming extends AbstractEntityTest {
    private Integer id1;
    private Integer id2;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(NamingTestEntity1.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        NamingTestEntity1 nte1 = new NamingTestEntity1("data1");
        NamingTestEntity1 nte2 = new NamingTestEntity1("data2");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(nte1);
        em.persist(nte2);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        nte1 = em.find(NamingTestEntity1.class, nte1.getId());
        nte1.setData("data1'");

        em.getTransaction().commit();

        // Revision 3
        em.getTransaction().begin();

        nte2 = em.find(NamingTestEntity1.class, nte2.getId());
        nte2.setData("data2'");

        em.getTransaction().commit();

        //

        id1 = nte1.getId();
        id2 = nte2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(NamingTestEntity1.class, id1));

        assert Arrays.asList(1, 3).equals(getVersionsReader().getRevisions(NamingTestEntity1.class, id2));
    }

    @Test
    public void testHistoryOfId1() {
        NamingTestEntity1 ver1 = new NamingTestEntity1(id1, "data1");
        NamingTestEntity1 ver2 = new NamingTestEntity1(id1, "data1'");

        assert getVersionsReader().find(NamingTestEntity1.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(NamingTestEntity1.class, id1, 2).equals(ver2);
        assert getVersionsReader().find(NamingTestEntity1.class, id1, 3).equals(ver2);
    }

    @Test
    public void testHistoryOfId2() {
        NamingTestEntity1 ver1 = new NamingTestEntity1(id2, "data2");
        NamingTestEntity1 ver2 = new NamingTestEntity1(id2, "data2'");

        assert getVersionsReader().find(NamingTestEntity1.class, id2, 1).equals(ver1);
        assert getVersionsReader().find(NamingTestEntity1.class, id2, 2).equals(ver1);
        assert getVersionsReader().find(NamingTestEntity1.class, id2, 3).equals(ver2);
    }

    @Test
    public void testTableName() {
        assert "naming_test_entity_1_versions".equals(
                getCfg().getClassMapping("org.jboss.envers.test.integration.naming.NamingTestEntity1_versions")
                        .getTable().getName());
    }
}
