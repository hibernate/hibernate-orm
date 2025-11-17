/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import org.hibernate.dialect.HANADialect;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = HANAOptimisticLockingTest.SomeEntity.class)
@SessionFactory
@JiraKey(value = "HHH-11656")
@RequiresDialect(HANADialect.class)
public class HANAOptimisticLockingTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testOptimisticLock(SessionFactoryScope scope) {
		testWithSpecifiedLockMode( scope, LockModeType.OPTIMISTIC );
	}

	@Test
	public void testOptimisticLockForceIncrement(SessionFactoryScope scope) {
		testWithSpecifiedLockMode( scope, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
	}

	private void testWithSpecifiedLockMode(SessionFactoryScope scope, LockModeType lockModeType) {
		// makes sure we have an entity to actually query
		Object id = scope.fromTransaction(session -> {
			SomeEntity someEntity = new SomeEntity();
			session.persist( someEntity );
			return someEntity.getId();
		} );

		// tests that both the query execution doesn't throw a SQL syntax (which is the main bug) and that
		// the query returns an expected entity object.
		scope.inTransaction( session -> {
			/*
			 * This generates the wrong SQL query for HANA.
			 * Using optimistic lock and string query cause a bug.
			 *
			 * Generated SQL query for HANA is as follows:
			 *
			 * SELECT
			 * 		someentity0_.id as id1_0_,
			 * 		someentity0_.version as version2_0_
			 *   FROM SomeEntity someentity0_
			 *  WHERE someentity0_ = 1 of someentity0_.id
			 *
			 * The exception thrown by HANA is:
			 * com.sap.db.jdbc.exceptions.JDBCDriverException: SAP DBTech JDBC: [257]:
			 *   sql syntax error: incorrect syntax near "of": line 1
			 *
			 */
			SomeEntity entity = session
					.createQuery( "SELECT e FROM SomeEntity e WHERE e.id = :id", SomeEntity.class )
					.setParameter( "id", id )
					.setLockMode( lockModeType )
					.uniqueResult();

			assertNotNull( entity );
		} );
	}

	@Entity(name = "SomeEntity")
	public static class SomeEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Version
		private Integer version;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}
	}
}
