/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.exceptionhandling;

import java.sql.SQLException;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import org.hibernate.JDBCException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@TestForIssue( jiraKey = "HHH-13737")
@RequiresDialect(H2Dialect.class)
public class NonActiveTransactionSessionFindJdbcExceptionHandlingTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testJdbcExceptionThrown() {
		// delete "description" column so that a JDBCException caused by a SQLException is thrown when looking up the AnEntity
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					entityManager.createNativeQuery( "alter table AnEntity drop column description" ).executeUpdate();
				}
		);

		EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.find( AnEntity.class, 1 );
			fail( "A PersistenceException should have been thrown." );
		}
		catch ( PersistenceException ex ) {
			assertTrue( JDBCException.class.isInstance( ex.getCause() ) );
			assertTrue( SQLException.class.isInstance( ex.getCause().getCause() ) );
		}
		finally {
			entityManager.close();
		}
	}

	@Before
	public void setupData() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					entityManager.persist( new AnEntity( 1, "description" ) );
				}
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, true);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;
		@Column(name = "description")
		private String description;

		AnEntity() {
		}

		AnEntity(int id, String description) {
			this.id = id;
			this.description = description;
		}
	}
}
