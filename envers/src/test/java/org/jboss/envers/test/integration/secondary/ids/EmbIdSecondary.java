package org.jboss.envers.test.integration.secondary.ids;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.ids.EmbId;
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
public class EmbIdSecondary extends AbstractEntityTest {
    private EmbId id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(SecondaryEmbIdTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        id = new EmbId(1, 2);

        SecondaryEmbIdTestEntity ste = new SecondaryEmbIdTestEntity(id, "a", "1");

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(ste);

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        ste = em.find(SecondaryEmbIdTestEntity.class, ste.getId());
        ste.setS1("b");
        ste.setS2("2");

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(SecondaryEmbIdTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId() {
        SecondaryEmbIdTestEntity ver1 = new SecondaryEmbIdTestEntity(id, "a", "1");
        SecondaryEmbIdTestEntity ver2 = new SecondaryEmbIdTestEntity(id, "b", "2");

        assert getVersionsReader().find(SecondaryEmbIdTestEntity.class, id, 1).equals(ver1);
        assert getVersionsReader().find(SecondaryEmbIdTestEntity.class, id, 2).equals(ver2);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testTableNames() {
        assert "sec_embid_versions".equals(((Iterator<Join>)
                getCfg().getClassMapping("org.jboss.envers.test.integration.secondary.ids.SecondaryEmbIdTestEntity_versions")
                        .getJoinIterator())
                .next().getTable().getName());
    }
}