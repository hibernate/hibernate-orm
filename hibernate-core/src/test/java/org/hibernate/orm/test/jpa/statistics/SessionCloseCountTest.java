/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.statistics;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11602")
@Jpa(
		integrationSettings = { @Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true") }
)
public class SessionCloseCountTest {

	@Test
	public void sessionCountClosetShouldBeIncrementedWhenTheEntityManagerIsClosed(EntityManagerFactoryScope scope) {
		final EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();
		final Statistics statistics = entityManagerFactory.unwrap( SessionFactory.class ).getStatistics();
		scope.inEntityManager(
				entityManager -> {
					assertThat( "The session close count should be zero", statistics.getSessionCloseCount(), is( 0L ) );

					entityManager.close();

					assertThat( "The session close count was not incremented", statistics.getSessionCloseCount(), is( 1L ) );

				}
		);
	}
}
