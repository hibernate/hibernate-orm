package org.jboss.envers.test.integration.revfordate;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrTestEntity;
import org.jboss.envers.exception.RevisionDoesNotExistException;
import org.jboss.envers.VersionsReader;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionForDate extends AbstractEntityTest {
    private long timestamp1;
    private long timestamp2;
    private long timestamp3;
    private long timestamp4;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() throws InterruptedException {
        timestamp1 = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        StrTestEntity rfd = new StrTestEntity("x");
        em.persist(rfd);
        Integer id = rfd.getId();
        em.getTransaction().commit();

        timestamp2 = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 2
        em.getTransaction().begin();
        rfd = em.find(StrTestEntity.class, id);
        rfd.setStr("y");
        em.getTransaction().commit();

        timestamp3 = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 3
        em.getTransaction().begin();
        rfd = em.find(StrTestEntity.class, id);
        rfd.setStr("z");
        em.getTransaction().commit();

        timestamp4 = System.currentTimeMillis();
    }

    @Test(expectedExceptions = RevisionDoesNotExistException.class)
    public void testTimestamps1() {
        getVersionsReader().getRevisionNumberForDate(new Date(timestamp1));
    }

    @Test
    public void testTimestamps() {
        assert getVersionsReader().getRevisionNumberForDate(new Date(timestamp2)).intValue() == 1;
        assert getVersionsReader().getRevisionNumberForDate(new Date(timestamp3)).intValue() == 2;
        assert getVersionsReader().getRevisionNumberForDate(new Date(timestamp4)).intValue() == 3;
    }

    @Test
    public void testDatesForRevisions() {
        VersionsReader vr = getVersionsReader();
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(1)).intValue() == 1;
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(2)).intValue() == 2;
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(3)).intValue() == 3;
    }

    @Test
    public void testRevisionsForDates() {
        VersionsReader vr = getVersionsReader();

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2))).getTime() <= timestamp2;
        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2)).intValue()+1).getTime() > timestamp2;

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp3))).getTime() <= timestamp3;
        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp3)).intValue()+1).getTime() > timestamp3;

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp4))).getTime() <= timestamp4;
    }
}
