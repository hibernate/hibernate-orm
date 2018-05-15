/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

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
