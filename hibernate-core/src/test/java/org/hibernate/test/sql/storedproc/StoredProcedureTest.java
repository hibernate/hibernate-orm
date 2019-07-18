/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.storedproc;

import java.util.List;
import javax.persistence.ParameterMode;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { H2ProcTesting.MyEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		H2ProcTesting.applyProcDefinitions( configuration );
	}

	@Test
	public void baseTest() {
		Session session = openSession();
		session.beginTransaction();

		ProcedureCall procedureCall = session.createStoredProcedureCall( "user");
		ProcedureOutputs procedureOutputs = procedureCall.getOutputs();
		Output currentOutput = procedureOutputs.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		String name = (String) resultSetReturn.getSingleResult();
		assertEquals( "SA", name );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testGetSingleResultTuple() {
		Session session = openSession();
		session.beginTransaction();

		ProcedureCall query = session.createStoredProcedureCall( "findOneUser" );
		ProcedureOutputs procedureResult = query.getOutputs();
		Output currentOutput = procedureResult.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		Object result = resultSetReturn.getSingleResult();
		assertTyping( Object[].class, result );
		String name = (String) ( (Object[]) result )[1];
		assertEquals( "Steve", name );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testGetResultListTuple() {
		Session session = openSession();
		session.beginTransaction();

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

		session.getTransaction().commit();
		session.close();
	}
}
