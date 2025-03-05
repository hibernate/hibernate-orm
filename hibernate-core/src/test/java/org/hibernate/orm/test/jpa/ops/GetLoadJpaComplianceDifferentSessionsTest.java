/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@JiraKey("HHH-12273")
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
	@JiraKey("HHH-9856")
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

		Workload merged;
		try {
			merged = doInJPA( () -> newEntityManagerFactory, entityManager -> {
				Workload workload = entityManager.unwrap( Session.class ).merge( _workload );

				workload.getId();
				return workload;
			} );
		}
		finally {
			newEntityManagerFactory.close();
		}

		assertEquals( "Package", merged.getName() );
	}
}
