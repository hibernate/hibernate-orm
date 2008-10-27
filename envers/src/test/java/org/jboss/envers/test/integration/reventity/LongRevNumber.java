package org.jboss.envers.test.integration.reventity;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrTestEntity;
import org.jboss.envers.VersionsReader;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class LongRevNumber extends AbstractEntityTest {
    private Integer id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(LongRevNumberRevEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() throws InterruptedException {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        StrTestEntity te = new StrTestEntity("x");
        em.persist(te);
        id = te.getId();
        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();
        te = em.find(StrTestEntity.class, id);
        te.setStr("y");
        em.getTransaction().commit();
    }

    @Test
    public void testFindRevision() {
        VersionsReader vr = getVersionsReader();

        assert vr.findRevision(LongRevNumberRevEntity.class, 1l).getCustomId() == 1l;
        assert vr.findRevision(LongRevNumberRevEntity.class, 2l).getCustomId() == 2l;
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1l, 2l).equals(getVersionsReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId1() {
        StrTestEntity ver1 = new StrTestEntity("x", id);
        StrTestEntity ver2 = new StrTestEntity("y", id);

        assert getVersionsReader().find(StrTestEntity.class, id, 1l).equals(ver1);
        assert getVersionsReader().find(StrTestEntity.class, id, 2l).equals(ver2);
    }
}