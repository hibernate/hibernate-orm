package org.hibernate.envers.test.integration.reventity;

import javax.persistence.EntityManager;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6696")
public class GloballyConfiguredRevListenerTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.REVISION_LISTENER, CountingRevisionListener.class.getName() );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		CountingRevisionListener.revisionCount = 0;

		// Revision 1
		em.getTransaction().begin();
		StrTestEntity te = new StrTestEntity( "data" );
		em.persist( te );
		em.getTransaction().commit();

		Assert.assertEquals( 1, CountingRevisionListener.revisionCount );
	}
}
