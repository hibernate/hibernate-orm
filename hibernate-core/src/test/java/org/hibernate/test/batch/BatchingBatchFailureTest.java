/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.batch;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.lang.reflect.Field;
import java.util.Map;

import org.hibernate.HibernateException;
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

		//open secondary session to insert a valid entity and an invalid entity
		Session secondSession = session.sessionWithOptions()
				.connection()
				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();

		try {
			secondSession.persist( new User( 1, "ok" ) );
			secondSession.persist( new User( 2, null ) );
			// the flush should fail
			secondSession.flush();
			fail( "Expecting SQLException" );
		}
		catch (HibernateException expected) {
			secondSession.close();

			try {
				//at this point the transaction is still active but the batch should have been aborted (have to use reflection to get at the field)
				SessionImplementor sessionImplementor = (SessionImplementor) session;
				Field field = sessionImplementor.getTransactionCoordinator().getJdbcCoordinator().getClass().getDeclaredField( "currentBatch" );
				field.setAccessible( true );
				Batch batch = (Batch) field.get( sessionImplementor.getTransactionCoordinator().getJdbcCoordinator() );
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
		@Id
		private Integer id;
		@Column( nullable = false )
		private String name;

		public User() {
		}

		public User(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
