package org.jboss.envers.test.integration.collection.mapkey;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.tools.TestTools;
import org.jboss.envers.test.entities.StrTestEntity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class IdMapKey extends AbstractEntityTest {
    private Integer imke_id;

    private Integer ste1_id;
    private Integer ste2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IdMapKeyEntity.class);
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        EntityManager em = getEntityManager();

        IdMapKeyEntity imke = new IdMapKeyEntity();

        // Revision 1 (intialy 1 mapping)
        em.getTransaction().begin();

        StrTestEntity ste1 = new StrTestEntity("x");
        StrTestEntity ste2 = new StrTestEntity("y");

        em.persist(ste1);
        em.persist(ste2);

        imke.getIdmap().put(ste1.getId(), ste1);

        em.persist(imke);

        em.getTransaction().commit();

        // Revision 2 (sse1: adding 1 mapping)
        em.getTransaction().begin();

        ste2 = em.find(StrTestEntity.class, ste2.getId());
        imke = em.find(IdMapKeyEntity.class, imke.getId());

        imke.getIdmap().put(ste2.getId(), ste2);

        em.getTransaction().commit();

        //

        imke_id = imke.getId();

        ste1_id = ste1.getId();
        ste2_id = ste2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(IdMapKeyEntity.class, imke_id));
    }

    @Test
    public void testHistoryOfImke() {
        StrTestEntity ste1 = getEntityManager().find(StrTestEntity.class, ste1_id);
        StrTestEntity ste2 = getEntityManager().find(StrTestEntity.class, ste2_id);

        IdMapKeyEntity rev1 = getVersionsReader().find(IdMapKeyEntity.class, imke_id, 1);
        IdMapKeyEntity rev2 = getVersionsReader().find(IdMapKeyEntity.class, imke_id, 2);

        assert rev1.getIdmap().equals(TestTools.makeMap(ste1.getId(), ste1));
        assert rev2.getIdmap().equals(TestTools.makeMap(ste1.getId(), ste1, ste2.getId(), ste2));
    }
}