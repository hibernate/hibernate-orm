/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;

import jakarta.persistence.EntityNotFoundException;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 * @author Hardy Ferentschik
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@Jpa(
		annotatedClasses = {
				Workload.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.JPA_PROXY_COMPLIANCE, value = "true") },
		xmlMappings = { "org/hibernate/orm/test/jpa/ops/Node.hbm.xml", "org/hibernate/orm/test/jpa/ops/Employer.hbm.xml" }
)
public class GetLoadJpaComplianceTest {

	@Test
	@JiraKey(value = "HHH-12034")
	public void testLoadIdNotFound_FieldBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Session s = (Session) entityManager.getDelegate();

						assertNull( s.get( Workload.class, 999 ) );

						Workload proxy = s.getReference( Workload.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
						fail( "Should have failed because there is no Employee Entity with id == 999" );
					}
					catch (EntityNotFoundException ex) {
						// expected
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12034")
	public void testReferenceIdNotFound_FieldBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						assertNull( entityManager.find( Workload.class, 999 ) );

						Workload proxy = entityManager.getReference( Workload.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
						fail( "Should have failed because there is no Workload Entity with id == 999" );
					}
					catch (EntityNotFoundException ex) {
						// expected
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12034")
	public void testLoadIdNotFound_PropertyBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Session s = (Session) entityManager.getDelegate();

						assertNull( s.get( Employee.class, 999 ) );

						Employee proxy = s.getReference( Employee.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
						fail( "Should have failed because there is no Employee Entity with id == 999" );
					}
					catch (EntityNotFoundException ex) {
						// expected
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12034")
	public void testReferenceIdNotFound_PropertyBasedAccess(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();

						assertNull( entityManager.find( Employee.class, 999 ) );

						Employee proxy = entityManager.getReference( Employee.class, 999 );
						assertFalse( Hibernate.isInitialized( proxy ) );

						proxy.getId();
						fail( "Should have failed because there is no Employee Entity with id == 999" );
					}
					catch (EntityNotFoundException ex) {
						// expected
					}
					finally {
						entityManager.getTransaction().rollback();
					}
				}
		);
	}
}
