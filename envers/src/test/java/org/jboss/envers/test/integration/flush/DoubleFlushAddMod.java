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
public class DoubleFlushAddMod extends AbstractFlushTest {
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

        fe.setStr("y");

        em.flush();

        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();

        fe = em.find(StrTestEntity.class, fe.getId());

        fe.setStr("z");
        em.flush();

        em.getTransaction().commit();

        //

        id = fe.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId() {
        StrTestEntity ver1 = new StrTestEntity("y", id);
        StrTestEntity ver2 = new StrTestEntity("z", id);

        assert getVersionsReader().find(StrTestEntity.class, id, 1).equals(ver1);
        assert getVersionsReader().find(StrTestEntity.class, id, 2).equals(ver2);
    }
}