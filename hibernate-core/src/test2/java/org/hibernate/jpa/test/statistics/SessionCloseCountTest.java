/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.statistics;

import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11602")
public class SessionCloseCountTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void sessionCountClosetShouldBeIncrementedWhenTheEntityManagerIsClosed() {
		final SessionFactoryImplementor entityManagerFactory = entityManagerFactory();
		final Statistics statistics = entityManagerFactory.unwrap( SessionFactory.class ).getStatistics();
		EntityManager em = createEntityManager();
		assertThat( "The session close count should be zero", statistics.getSessionCloseCount(), is( 0L ) );

		em.close();

		assertThat( "The session close count was not incremented", statistics.getSessionCloseCount(), is( 1L ) );
	}
}
