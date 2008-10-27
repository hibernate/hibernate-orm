package org.jboss.envers.test.integration.flush;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hibernate.FlushMode;
import org.jboss.envers.test.entities.StrTestEntity;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleFlushAddDel extends AbstractFlushTest {
    private Integer id;

    public FlushMode getFlushMode() {
        return FlushMode.MANUAL;
    }

    @BeforeClass(dependsOnMethods = "initFlush")
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        StrTestEntity fe = new StrTestEntity("x");
        em.persist(fe);

        em.flush();

        em.remove(em.find(StrTestEntity.class, fe.getId()));

        em.flush();

        em.getTransaction().commit();

        //

        id = fe.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList().equals(getVersionsReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId() {
        assert getVersionsReader().find(StrTestEntity.class, id, 1) == null;
    }
}