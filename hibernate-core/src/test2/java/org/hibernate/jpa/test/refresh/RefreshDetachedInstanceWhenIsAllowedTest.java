/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.refresh;

import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11188")
public class RefreshDetachedInstanceWhenIsAllowedTest extends BaseEntityManagerFunctionalTestCase {
	private TestEntity testEntity;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.ALLOW_REFRESH_DETACHED_ENTITY, "true" );
	}

	@Before
	public void setUp() {
		testEntity = new TestEntity();
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.persist( testEntity );
		} );
	}

	@Test
	public void testUnwrappedSessionRefreshDetachedInstance() {
		final EntityManager entityManager = createEntityManager();
		final Session session = entityManager.unwrap( Session.class );
		session.refresh( testEntity );
	}

	@Test
	public void testRefreshDetachedInstance() {
		final EntityManager entityManager = createEntityManager();
		entityManager.refresh( testEntity );
	}
}
