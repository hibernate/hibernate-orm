package org.jboss.envers.test.integration.secondary;

import org.jboss.envers.test.AbstractEntityTest;
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
public class NamingSecondary extends AbstractEntityTest {
    private Integer id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SecondaryNamingTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        SecondaryNamingTestEntity ste = new SecondaryNamingTestEntity("a", "1");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ste);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ste = em.find(SecondaryNamingTestEntity.class, ste.getId());
        ste.setS1("b");
        ste.setS2("2");

        em.getTransaction().commit();

        //

        id = ste.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SecondaryNamingTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId() {
        SecondaryNamingTestEntity ver1 = new SecondaryNamingTestEntity(id, "a", "1");
        SecondaryNamingTestEntity ver2 = new SecondaryNamingTestEntity(id, "b", "2");

        assert getVersionsReader().find(SecondaryNamingTestEntity.class, id, 1).equals(ver1);
        assert getVersionsReader().find(SecondaryNamingTestEntity.class, id, 2).equals(ver2);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testTableNames() {
        assert "sec_versions".equals(((Iterator<Join>)
                getCfg().getClassMapping("org.jboss.envers.test.integration.secondary.SecondaryNamingTestEntity_versions")
                        .getJoinIterator())
                .next().getTable().getName());
    }
}
