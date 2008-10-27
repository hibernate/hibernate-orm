package org.jboss.envers.test.integration.secondary.ids;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.ids.MulId;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.mapping.Join;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MulIdSecondary extends AbstractEntityTest {
    private MulId id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SecondaryMulIdTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id = new MulId(1, 2);

        SecondaryMulIdTestEntity ste = new SecondaryMulIdTestEntity(id, "a", "1");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ste);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ste = em.find(SecondaryMulIdTestEntity.class, id);
        ste.setS1("b");
        ste.setS2("2");

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SecondaryMulIdTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId() {
        SecondaryMulIdTestEntity ver1 = new SecondaryMulIdTestEntity(id, "a", "1");
        SecondaryMulIdTestEntity ver2 = new SecondaryMulIdTestEntity(id, "b", "2");

        assert getVersionsReader().find(SecondaryMulIdTestEntity.class, id, 1).equals(ver1);
        assert getVersionsReader().find(SecondaryMulIdTestEntity.class, id, 2).equals(ver2);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testTableNames() {
        assert "sec_mulid_versions".equals(((Iterator<Join>)
                getCfg().getClassMapping("org.jboss.envers.test.integration.secondary.ids.SecondaryMulIdTestEntity_versions")
                        .getJoinIterator())
                .next().getTable().getName());
    }
}