package org.hibernate.envers.test.integration.inheritance.mixed;

import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.inheritance.mixed.entities.Activity;
import org.hibernate.envers.test.integration.inheritance.mixed.entities.CheckInActivity;
import org.hibernate.envers.test.integration.inheritance.mixed.entities.NormalActivity;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
public abstract class AbstractInheritanceStrategiesSessionTest extends AbstractSessionTest {
    @Test
    @Priority(10)
    public void initData() {
        NormalActivity normalActivity = new NormalActivity();
        normalActivity.setId(1);
        normalActivity.setSequenceNumber(1);

        // Revision 1
        getSession().getTransaction().begin();

        getSession().persist(normalActivity);

        getSession().getTransaction().commit();
        // Revision 2
        getSession().getTransaction().begin();

        normalActivity = (NormalActivity) getSession().get(NormalActivity.class, 1);
        CheckInActivity checkInActivity = new CheckInActivity();
        checkInActivity.setId(2);
        checkInActivity.setSequenceNumber(0);
        checkInActivity.setDurationInMinutes(30);
        checkInActivity.setRelatedActivity(normalActivity);

        getSession().persist(checkInActivity);

        getSession().getTransaction().commit();

        // Revision 3
        normalActivity = new NormalActivity();
        normalActivity.setId(3);
        normalActivity.setSequenceNumber(2);

        getSession().getTransaction().begin();

        getSession().persist(normalActivity);

        getSession().getTransaction().commit();

        // Revision 4
        getSession().getTransaction().begin();

        normalActivity = (NormalActivity) getSession().get(NormalActivity.class, 3);
        checkInActivity = (CheckInActivity) getSession().get(CheckInActivity.class, 2);
        checkInActivity.setRelatedActivity(normalActivity);

        getSession().merge(checkInActivity);

        getSession().getTransaction().commit();

        getSession().close();
    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(NormalActivity.class, 1));
        assertEquals(Arrays.asList(3), getAuditReader().getRevisions(NormalActivity.class, 3));
        assertEquals(Arrays.asList(2, 4), getAuditReader().getRevisions(CheckInActivity.class, 2));
    }

    @Test
    public void testCurrentStateOfCheckInActivity() {

        final CheckInActivity checkInActivity = (CheckInActivity) getSession().get(CheckInActivity.class, 2);
        final NormalActivity normalActivity = (NormalActivity) getSession().get(NormalActivity.class, 3);

        assertEquals(2, checkInActivity.getId().intValue());
        assertEquals(0, checkInActivity.getSequenceNumber().intValue());
        assertEquals(30, checkInActivity.getDurationInMinutes().intValue());
        final Activity relatedActivity = checkInActivity.getRelatedActivity();
        assertEquals(normalActivity.getId(), relatedActivity.getId());
        assertEquals(normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber());
    }

    @Test
    public void testCheckCurrentStateOfNormalActivities() throws Exception {
        final NormalActivity normalActivity1 = (NormalActivity) getSession().get(NormalActivity.class, 1);
        final NormalActivity normalActivity2 = (NormalActivity) getSession().get(NormalActivity.class, 3);

        assertEquals(1, normalActivity1.getId().intValue());
        assertEquals(1, normalActivity1.getSequenceNumber().intValue());
        assertEquals(3, normalActivity2.getId().intValue());
        assertEquals(2, normalActivity2.getSequenceNumber().intValue());
    }

    public void doTestFirstRevisionOfCheckInActivity() throws Exception {
        CheckInActivity checkInActivity = getAuditReader().find(CheckInActivity.class, 2, 2);
        NormalActivity normalActivity = getAuditReader().find(NormalActivity.class, 1, 2);

        assertEquals(2, checkInActivity.getId().intValue());
        assertEquals(0, checkInActivity.getSequenceNumber().intValue());
        assertEquals(30, checkInActivity.getDurationInMinutes().intValue());
        Activity relatedActivity = checkInActivity.getRelatedActivity();
        assertEquals(normalActivity.getId(), relatedActivity.getId());
        assertEquals(normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber());
    }

    public void doTestSecondRevisionOfCheckInActivity() throws Exception {
        CheckInActivity checkInActivity = getAuditReader().find(CheckInActivity.class, 2, 4);
        NormalActivity normalActivity = getAuditReader().find(NormalActivity.class, 3, 4);

        assertEquals(2, checkInActivity.getId().intValue());
        assertEquals(0, checkInActivity.getSequenceNumber().intValue());
        assertEquals(30, checkInActivity.getDurationInMinutes().intValue());
        Activity relatedActivity = checkInActivity.getRelatedActivity();
        assertEquals(normalActivity.getId(), relatedActivity.getId());
        assertEquals(normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber());
    }
}
