package org.jboss.envers.test.integration.basic;

import org.jboss.envers.test.AbstractEntityTest;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UnversionedPropertiesChange extends AbstractEntityTest {
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(BasicTestEntity2.class);
    }

    private Integer addNewEntity(String str1, String str2) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity2 bte2 = new BasicTestEntity2(str1, str2);
        em.persist(bte2);
        em.getTransaction().commit();

        return bte2.getId();
    }

    private void modifyEntity(Integer id, String str1, String str2) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        BasicTestEntity2 bte2 = em.find(BasicTestEntity2.class, id);
        bte2.setStr1(str1);
        bte2.setStr2(str2);
        em.getTransaction().commit();
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id1 = addNewEntity("x", "a"); // rev 1
        modifyEntity(id1, "x", "a"); // no rev
        modifyEntity(id1, "y", "b"); // rev 2
        modifyEntity(id1, "y", "c"); // no rev
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(BasicTestEntity2.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        BasicTestEntity2 ver1 = new BasicTestEntity2(id1, "x", null);
        BasicTestEntity2 ver2 = new BasicTestEntity2(id1, "y", null);

        assert getVersionsReader().find(BasicTestEntity2.class, id1, 1).equals(ver1);
        assert getVersionsReader().find(BasicTestEntity2.class, id1, 2).equals(ver2);
    }
}
