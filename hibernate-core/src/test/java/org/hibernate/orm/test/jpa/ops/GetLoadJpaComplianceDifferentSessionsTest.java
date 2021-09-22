/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ops;

import java.util.Map;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;

import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12273")
public class GetLoadJpaComplianceDifferentSessionsTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Workload.class,
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_PROXY_COMPLIANCE, Boolean.FALSE.toString() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9856")
	public void testReattachEntityToSessionWithJpaComplianceProxy() {
		final Integer _workloadId = fromTransaction( entityManager -> {
			Workload workload = new Workload();
			workload.load = 123;
			workload.name = "Package";
			entityManager.persist( workload );

			return workload.getId();
		} );

		Workload _workload = fromTransaction(
				entityManager ->
						entityManager.getReference( Workload.class, _workloadId )
		);

		Map settings = buildSettings();
		settings.put( AvailableSettings.JPA_PROXY_COMPLIANCE, Boolean.TRUE.toString() );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "none" );

		EntityManagerFactory newEntityManagerFactory = Bootstrap
				.getEntityManagerFactoryBuilder(
						new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
						settings
				)
				.build();

		try {
			doInJPA( () -> newEntityManagerFactory, entityManager -> {
				entityManager.unwrap( Session.class ).update( _workload );

				_workload.getId();
			} );
		}
		finally {
			newEntityManagerFactory.close();
		}

		assertEquals( "Package", _workload.getName() );
	}
}
