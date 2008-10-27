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
public class BasicSecondary extends AbstractEntityTest {
    private Integer id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SecondaryTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        SecondaryTestEntity ste = new SecondaryTestEntity("a", "1");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ste);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ste = em.find(SecondaryTestEntity.class, ste.getId());
        ste.setS1("b");
        ste.setS2("2");

        em.getTransaction().commit();

        //

        id = ste.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SecondaryTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId() {
        SecondaryTestEntity ver1 = new SecondaryTestEntity(id, "a", "1");
        SecondaryTestEntity ver2 = new SecondaryTestEntity(id, "b", "2");

        assert getVersionsReader().find(SecondaryTestEntity.class, id, 1).equals(ver1);
        assert getVersionsReader().find(SecondaryTestEntity.class, id, 2).equals(ver2);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testTableNames() {
        assert "secondary_versions".equals(((Iterator<Join>)
                getCfg().getClassMapping("org.jboss.envers.test.integration.secondary.SecondaryTestEntity_versions")
                        .getJoinIterator())
                .next().getTable().getName());
    }
}