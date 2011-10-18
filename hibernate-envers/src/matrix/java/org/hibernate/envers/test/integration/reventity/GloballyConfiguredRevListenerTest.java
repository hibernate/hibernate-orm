package org.hibernate.envers.test.integration.reventity;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6696")
public class GloballyConfiguredRevListenerTest extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.setProperty("org.hibernate.envers.revision_listener", "org.hibernate.envers.test.integration.reventity.CountingRevisionListener");
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();

        CountingRevisionListener.revisionCount = 0;

        // Revision 1
        em.getTransaction().begin();
        StrTestEntity te = new StrTestEntity("data");
        em.persist(te);
        em.getTransaction().commit();

        Assert.assertEquals(1, CountingRevisionListener.revisionCount);
    }
}
