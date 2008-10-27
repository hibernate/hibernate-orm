package org.jboss.envers.test.integration.reventity;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrTestEntity;
import org.jboss.envers.exception.RevisionDoesNotExistException;
import org.jboss.envers.VersionsReader;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.Arrays;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class Listener extends AbstractEntityTest {
    private Integer id;
    private long timestamp1;
    private long timestamp2;
    private long timestamp3;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(ListenerRevEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() throws InterruptedException {
        timestamp1 = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        TestRevisionListener.data = "data1";

        StrTestEntity te = new StrTestEntity("x");
        em.persist(te);
        id = te.getId();
        em.getTransaction().commit();

        timestamp2 = System.currentTimeMillis();

        Thread.sleep(100);

        // Revision 2
        em.getTransaction().begin();
        te = em.find(StrTestEntity.class, id);

        TestRevisionListener.data = "data2";

        te.setStr("y");
        em.getTransaction().commit();

        timestamp3 = System.currentTimeMillis();
    }

    @Test(expectedExceptions = RevisionDoesNotExistException.class)
    public void testTimestamps1() {
        getVersionsReader().getRevisionNumberForDate(new Date(timestamp1));
    }

    @Test
    public void testTimestamps() {
        assert getVersionsReader().getRevisionNumberForDate(new Date(timestamp2)).intValue() == 1;
        assert getVersionsReader().getRevisionNumberForDate(new Date(timestamp3)).intValue() == 2;
    }

    @Test
    public void testDatesForRevisions() {
        VersionsReader vr = getVersionsReader();
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(1)).intValue() == 1;
        assert vr.getRevisionNumberForDate(vr.getRevisionDate(2)).intValue() == 2;
    }

    @Test
    public void testRevisionsForDates() {
        VersionsReader vr = getVersionsReader();

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2))).getTime() <= timestamp2;
        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp2)).intValue()+1).getTime() > timestamp2;

        assert vr.getRevisionDate(vr.getRevisionNumberForDate(new Date(timestamp3))).getTime() <= timestamp3;
    }

    @Test
    public void testFindRevision() {
        VersionsReader vr = getVersionsReader();

        ListenerRevEntity rev1Data = vr.findRevision(ListenerRevEntity.class, 1);
        ListenerRevEntity rev2Data = vr.findRevision(ListenerRevEntity.class, 2);

        long rev1Timestamp = rev1Data.getTimestamp();
        assert rev1Timestamp > timestamp1;
        assert rev1Timestamp <= timestamp2;

        assert "data1".equals(rev1Data.getData());

        long rev2Timestamp = rev2Data.getTimestamp();
        assert rev2Timestamp > timestamp2;
        assert rev2Timestamp <= timestamp3;

        assert "data2".equals(rev2Data.getData());
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getVersionsReader().getRevisions(StrTestEntity.class, id));
    }

    @Test
    public void testHistoryOfId1() {
        StrTestEntity ver1 = new StrTestEntity("x", id);
        StrTestEntity ver2 = new StrTestEntity("y", id);

        assert getVersionsReader().find(StrTestEntity.class, id, 1).equals(ver1);
        assert getVersionsReader().find(StrTestEntity.class, id, 2).equals(ver2);
    }
}
