package org.hibernate.envers.test.integration.reventity.trackmodifiedentities;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class TrackingEntitiesMultipleChangesTest extends AbstractEntityTest {
    private Integer steId1 = null;
    private Integer steId2 = null;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.setProperty("org.hibernate.envers.track_entities_changed_in_revision", "true");
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();

        // Revision 1 - Adding two entities
        em.getTransaction().begin();
        StrTestEntity ste1 = new StrTestEntity("x");
        StrTestEntity ste2 = new StrTestEntity("y");
        em.persist(ste1);
        em.persist(ste2);
        steId1 = ste1.getId();
        steId2 = ste2.getId();
        em.getTransaction().commit();

        // Revision 2 - Adding first and removing second entity
        em.getTransaction().begin();
        ste1 = em.find(StrTestEntity.class, steId1);
        ste2 = em.find(StrTestEntity.class, steId2);
        ste1.setStr("z");
        em.remove(ste2);
        em.getTransaction().commit();

        // Revision 3 - Modifying and removing the same entity.
        em.getTransaction().begin();
        ste1 = em.find(StrTestEntity.class, steId1);
        ste1.setStr("a");
        em.merge(ste1);
        em.remove(ste1);
        em.getTransaction().commit();
    }

    @Test
    public void testTrackAddedTwoEntities() {
        StrTestEntity ste1 = new StrTestEntity("x", steId1);
        StrTestEntity ste2 = new StrTestEntity("y", steId2);

        assert Arrays.asList(ste1, ste2).equals(getCrossTypeRevisionChangesReader().findEntities(1));
    }

    @Test
    public void testTrackUpdateAndRemoveDifferentEntities() {
        StrTestEntity ste1 = new StrTestEntity("z", steId1);
        StrTestEntity ste2 = new StrTestEntity(null, steId2);

        assert Arrays.asList(ste1, ste2).equals(getCrossTypeRevisionChangesReader().findEntities(2));
    }

    @Test
    public void testTrackUpdateAndRemoveTheSameEntity() {
        StrTestEntity ste1 = new StrTestEntity(null, steId1);

        assert Arrays.asList(ste1).equals(getCrossTypeRevisionChangesReader().findEntities(3));
    }

    private CrossTypeRevisionChangesReader getCrossTypeRevisionChangesReader() {
        return getAuditReader().getCrossTypeRevisionChangesReader();
    }
}