/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batch;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.lang.reflect.Field;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.batch.internal.AbstractBatchImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.spi.SessionImplementor;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Shawn Clowater
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7689" )
public class BatchingBatchFailureTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		// explicitly enable batching
		configuration.setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, "5" );
		// and disable in-vm nullability checking (so we can force in-db not-null constraint violations)
		configuration.setProperty( AvailableSettings.CHECK_NULLABILITY, "false" );
	}

	@Test
	public void testBasicInsertion() {
		Session session = openSession();
		session.getTransaction().begin();

		try {
			session.persist( new User( 1, "ok" ) );
			session.persist( new User( 2, null ) );
			session.persist( new User( 3, "ok" ) );
			session.persist( new User( 4, "ok" ) );
			session.persist( new User( 5, "ok" ) );
			session.persist( new User( 6, "ok" ) );
			// the flush should fail
			session.flush();
			fail( "Expecting failed flush" );
		}
		catch (Exception expected) {
			System.out.println( "Caught expected exception : " + expected );
			expected.printStackTrace( System.out );

			try {
				//at this point the transaction is still active but the batch should have been aborted (have to use reflection to get at the field)
				SessionImplementor sessionImplementor = (SessionImplementor) session;
				Field field = sessionImplementor.getJdbcCoordinator().getClass().getDeclaredField( "currentBatch" );
				field.setAccessible( true );
				Batch batch = (Batch) field.get( sessionImplementor.getJdbcCoordinator() );
				if ( batch == null ) {
					throw new Exception( "Current batch was null" );
				}
				else {
					//make sure it's actually a batching impl
					assertEquals( BatchingBatch.class, batch.getClass() );
					field = AbstractBatchImpl.class.getDeclaredField( "statements" );
					field.setAccessible( true );
					//check to see that there aren't any statements queued up (this can be an issue if using SavePoints)
					assertEquals( 0, ((Map) field.get( batch )).size() );
				}
			}
			catch (Exception fieldException) {
				fail( "Couldn't inspect field " + fieldException.getMessage() );
			}
		}
		finally {
			session.getTransaction().rollback();
			session.close();
		}
	}

	@Entity( name = "User" )
	@Table( name = "`USER`" )
	public static class User {
		private Integer id;
		private String name;

		public User() {
		}

		public User(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Column( nullable = false )
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
