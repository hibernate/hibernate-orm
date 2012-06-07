package org.hibernate.envers.test.integration.reventity;

import java.util.Map;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6696")
public class GloballyConfiguredRevListenerTest extends BaseEnversJPAFunctionalTestCase {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put("org.hibernate.envers.revision_listener", "org.hibernate.envers.test.integration.reventity.CountingRevisionListener");
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
