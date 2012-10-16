package org.hibernate.sharedSession;

import junit.framework.Assert;
import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.internal.AbstractBatchImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

public class SessionWithSharedConnectionBatchTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( Environment.STATEMENT_BATCH_SIZE, "20" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IrrelevantEntity.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7689" )
	@FailureExpected( jiraKey = "HHH-7989" )
	public void testChildSessionAbortsBatchOnFailure() {
		Session session = openSession();
		session.getTransaction().begin();

		//open secondary session to insert a valid entity and an invalid entity
		Session secondSession = session.sessionWithOptions()
				.connection()
				.flushBeforeCompletion( true )
				.autoClose( true )
				.openSession();

		IrrelevantEntity validIrrelevantEntity = new IrrelevantEntity();
		validIrrelevantEntity.setName( "valid entity" );
		secondSession.save( validIrrelevantEntity );

		//name is required
		IrrelevantEntity invalidIrrelevantEntity = new IrrelevantEntity();
		secondSession.save( invalidIrrelevantEntity );

		try {
			secondSession.flush();
			secondSession.close();
			Assert.fail( "expected validation failure didn't occur" );
		}
		catch (Exception e) {
			secondSession.close();

			try {
				//at this point the transaction is still active but the batch should have been aborted (have to use reflection to get at the field)
				Field field = ( ( SessionImplementor ) session ).getTransactionCoordinator().getJdbcCoordinator().getClass().getDeclaredField( "currentBatch" );
				field.setAccessible( true );
				Batch batch = ( Batch ) field.get( ( ( SessionImplementor ) session ).getTransactionCoordinator().getJdbcCoordinator() );
				if ( batch == null ) {
					throw new Exception( "Current batch was null" );
				}
				else {
					//make sure it's actually a batching impl
					Assert.assertEquals( BatchingBatch.class, batch.getClass() );
					field = AbstractBatchImpl.class.getDeclaredField( "statements" );
					field.setAccessible( true );
					//check to see that there aren't any statements queued up (this can be an issue if using SavePoints)
					Assert.assertEquals( 0, ( ( Map ) field.get( batch ) ).size() );
				}
			}
			catch (Exception fieldException) {
				Assert.fail( "Couldn't inspect field " + fieldException.getMessage() );
			}
		}
		finally {
			session.getTransaction().rollback();
			session.close();
		}
	}
}
