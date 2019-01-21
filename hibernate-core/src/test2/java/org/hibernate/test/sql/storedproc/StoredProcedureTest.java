/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.storedproc;

import java.sql.CallableStatement;
import java.util.List;
import java.util.Map;
import javax.persistence.ParameterMode;

import org.hibernate.JDBCException;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.CallableStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureTest extends BaseNonConfigCoreFunctionalTestCase {

	private CallableStatementSpyConnectionProvider connectionProvider = new CallableStatementSpyConnectionProvider( true, true );

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.DISCARD_PC_ON_CLOSE, "false");
		settings.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		for ( AuxiliaryDatabaseObject auxiliaryDatabaseObject : H2ProcTesting.getAuxiliaryDatabaseObjects() ) {
			metadataBuilder.applyAuxiliaryDatabaseObject(
					auxiliaryDatabaseObject
			);
		}
	}

	@Override
	protected void prepareTest() throws Exception {
		connectionProvider.clear();
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { H2ProcTesting.MyEntity.class };
	}

	@Test
	public void baseTest() {
		doInHibernate( this::sessionFactory, session -> {
			ProcedureCall procedureCall = session.createStoredProcedureCall( "user");
			ProcedureOutputs procedureOutputs = procedureCall.getOutputs();
			Output currentOutput = procedureOutputs.getCurrent();
			assertNotNull( currentOutput );
			ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
			String name = (String) resultSetReturn.getSingleResult();
			assertEquals( "SA", name );
		} );
	}

	@Test
	public void testGetSingleResultTuple() {
		doInHibernate( this::sessionFactory, session -> {
			ProcedureCall query = session.createStoredProcedureCall( "findOneUser" );
			ProcedureOutputs procedureResult = query.getOutputs();
			Output currentOutput = procedureResult.getCurrent();
			assertNotNull( currentOutput );
			ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
			Object result = resultSetReturn.getSingleResult();
			assertTyping( Object[].class, result );
			String name = (String) ( (Object[]) result )[1];
			assertEquals( "Steve", name );
		} );
	}

	@Test
	public void testGetResultListTuple() {
		doInHibernate( this::sessionFactory, session -> {
			ProcedureCall query = session.createStoredProcedureCall( "findUsers" );
			ProcedureOutputs procedureResult = query.getOutputs();
			Output currentOutput = procedureResult.getCurrent();
			assertNotNull( currentOutput );
			ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
			List results = resultSetReturn.getResultList();
			assertEquals( 3, results.size() );

			for ( Object result : results ) {
				assertTyping( Object[].class, result );
				Integer id = (Integer) ( (Object[]) result )[0];
				String name = (String) ( (Object[]) result )[1];
				if ( id.equals( 1 ) ) {
					assertEquals( "Steve", name );
				}
				else if ( id.equals( 2 ) ) {
					assertEquals( "John", name );
				}
				else if ( id.equals( 3 ) ) {
					assertEquals( "Jane", name );
				}
				else {
					fail( "Unexpected id value found [" + id + "]" );
				}
			}
		} );
	}

// A warning should be logged if database metadata indicates named parameters are not supported.
	@Test
	public void testInParametersByName() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
				findUserRange(query);

				CallableStatement callableStatement = connectionProvider.getPreparedStatement( "{call findUserRange(?,?)}");
				verify(callableStatement, never()).close();
			} );
		} );
	}

	@Test
	public void testInParametersByNameWithStatementClose() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				try(ProcedureCall query = session.createStoredProcedureCall( "findUserRange" )) {
					findUserRange(query);
				}

				CallableStatement callableStatement = connectionProvider.getPreparedStatement( "{call findUserRange(?,?)}");
				verify(callableStatement, times(1)).close();
			} );
		} );
	}

	private void findUserRange(ProcedureCall query) {
		query.registerParameter( "start", Integer.class, ParameterMode.IN ).bindValue( 1 );
		query.registerParameter( "end", Integer.class, ParameterMode.IN ).bindValue( 2 );
		ProcedureOutputs procedureResult = query.getOutputs();
		Output currentOutput = procedureResult.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		List results = resultSetReturn.getResultList();
		assertEquals( 1, results.size() );
		Object result = results.get( 0 );
		assertTyping( Object[].class, result );
		Integer id = (Integer) ( (Object[]) result )[0];
		String name = (String) ( (Object[]) result )[1];
		assertEquals( 1, (int) id );
		assertEquals( "User 1", name );
	}

	@Test
	public void testInParametersByPosition() {
		doInHibernate( this::sessionFactory, session -> {
			ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
			query.registerParameter( 1, Integer.class, ParameterMode.IN )
					.bindValue( 1 );
			query.registerParameter( 2, Integer.class, ParameterMode.IN )
					.bindValue( 2 );
			ProcedureOutputs procedureResult = query.getOutputs();
			Output currentOutput = procedureResult.getCurrent();
			assertNotNull( currentOutput );
			ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
			List results = resultSetReturn.getResultList();
			assertEquals( 1, results.size() );
			Object result = results.get( 0 );
			assertTyping( Object[].class, result );
			Integer id = (Integer) ( (Object[]) result )[0];
			String name = (String) ( (Object[]) result )[1];
			assertEquals( 1, (int) id );
			assertEquals( "User 1", name );
		} );
	}

	@Test
	public void testInParametersNotSet() {
		doInHibernate( this::sessionFactory, session -> {
			// since the procedure does not define defaults for parameters this should result in SQLExceptions on
			// execution
			{
				ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
				query.registerParameter( 1, Integer.class, ParameterMode.IN );
				query.registerParameter( 2, Integer.class, ParameterMode.IN ).bindValue( 2 );
				try {
					query.getOutputs();
					fail( "Expecting failure due to missing parameter bind" );
				}
				catch (JDBCException expected) {
				}
			}

			// H2 does not support named parameters
			/*{
				ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
				query.registerParameter( "start", Integer.class, ParameterMode.IN );
				query.registerParameter( "end", Integer.class, ParameterMode.IN ).bindValue( 2 );
				try {
					query.getOutputs();
					fail( "Expecting failure due to missing parameter bind" );
				}
				catch (JDBCException expected) {
				}
			}*/
		} );
	}

	@Test
	public void testInParametersNotSetPass() {
		doInHibernate( this::sessionFactory, session -> {
			// unlike #testInParametersNotSet here we are asking that the NULL be passed
			// so these executions should succeed


			ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
			query.registerParameter( 1, Integer.class, ParameterMode.IN ).enablePassingNulls( true );
			query.registerParameter( 2, Integer.class, ParameterMode.IN ).bindValue( 2 );
			query.getOutputs();

			// H2 does not support named parameters
			/*{
				ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
				query.registerParameter( "start", Integer.class, ParameterMode.IN );
				query.registerParameter( "end", Integer.class, ParameterMode.IN ).bindValue( 2 );
				try {
					query.getOutputs();
					fail( "Expecting failure due to missing parameter bind" );
				}
				catch (JDBCException expected) {
				}
			}*/
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInParametersNullnessPassingInNamedQueriesViaHints() {
		doInHibernate( this::sessionFactory, session -> {
			// similar to #testInParametersNotSet and #testInParametersNotSetPass in terms of testing
			// support for specifying whether to pass NULL argument values or not.  This version tests
			// named procedure support via hints.

			// first a fixture - this execution should fail
			{
				ProcedureCall query = session.getNamedProcedureCall( "findUserRangeNoNullPassing" );
				query.getParameterRegistration( 2 ).bindValue( 2 );
				try {
					query.getOutputs();
					fail( "Expecting failure due to missing parameter bind" );
				}
				catch (JDBCException ignore) {
				}
			}

			// here we enable NULL passing via hint through a named parameter
			{
				ProcedureCall query = session.getNamedProcedureCall( "findUserRangeNamedNullPassing" );
				query.getParameterRegistration( "secondArg" ).bindValue( 2 );
				query.getOutputs();
			}

			// here we enable NULL passing via hint through a named parameter
			{
				ProcedureCall query = session.getNamedProcedureCall( "findUserRangeOrdinalNullPassing" );
				query.getParameterRegistration( 2 ).bindValue( 2 );
				query.getOutputs();
			}
		} );
	}

}
