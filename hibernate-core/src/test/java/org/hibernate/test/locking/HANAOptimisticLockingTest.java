/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.locking;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Version;

import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HANARowStoreDialect;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialects;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11656")
@RequiresDialects( { @RequiresDialect(HANAColumnStoreDialect.class), @RequiresDialect(HANARowStoreDialect.class) })
public class HANAOptimisticLockingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SomeEntity.class };
	}

	@Test
	public void testOptimisticLock() throws Exception {
		testWithSpecifiedLockMode( LockModeType.OPTIMISTIC );
	}

	@Test
	public void testOptimisticLockForceIncrement() throws Exception {
		testWithSpecifiedLockMode( LockModeType.OPTIMISTIC_FORCE_INCREMENT );
	}

	private void testWithSpecifiedLockMode(LockModeType lockModeType) {
		// makes sure we have an entity to actually query
		final Object id = doInHibernate( this::sessionFactory, session -> {
			return session.save( new SomeEntity() );
		} );

		// tests that both the query execution doesn't throw a SQL syntax (which is the main bug) and that
		// the query returns an expected entity object.
		doInHibernate( this::sessionFactory, session -> {
			/**
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
