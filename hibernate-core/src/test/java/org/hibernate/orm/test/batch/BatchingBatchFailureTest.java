/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.lang.reflect.Field;

import org.hibernate.SessionEventListener;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Shawn Clowater
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-7689" )
public class BatchingBatchFailureTest extends BaseCoreFunctionalTestCase implements SessionEventListener {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		// explicitly enable batching
		configuration.setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, 5 );
		// and disable in-vm nullability checking (so we can force in-db not-null constraint violations)
		configuration.setProperty( AvailableSettings.CHECK_NULLABILITY, false );
	}

	SessionImplementor session;
	Batch batch;

	@Override
	public void jdbcExecuteBatchStart() {
		try {
			Field field = session.getJdbcCoordinator().getClass().getDeclaredField( "currentBatch" );
			field.setAccessible( true );
			batch = (Batch) field.get( session.getJdbcCoordinator() );
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testBasicInsertion() {
		session = sessionFactory().withOptions().eventListeners( this ).openSession();
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
				if ( batch == null ) {
					throw new Exception( "Current batch was null" );
				}
				else {
//					//check to see that there aren't any statements queued up (this can be an issue if using SavePoints)
					final PreparedStatementDetails statementDetails = batch.getStatementGroup().getSingleStatementDetails();
					assertThat( statementDetails.getStatement() ).isNull();
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
