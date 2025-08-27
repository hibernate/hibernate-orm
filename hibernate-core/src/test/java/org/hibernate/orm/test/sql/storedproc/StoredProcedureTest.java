/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.storedproc;

import java.util.List;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ParameterMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class StoredProcedureTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				H2ProcTesting.MyEntity.class
		};
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.applyMetadataBuilder( metadataBuilder );
		H2ProcTesting.applyProcDefinitions( metadataBuilder );
	}

	@Test
	public void baseTest() {
		inTransaction( session -> {
			ProcedureCall procedureCall = session.createStoredProcedureCall( "user" );
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
		inTransaction( session -> {
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
		inTransaction( session -> {
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

	@Test
	@FailureExpected( reason = "We currently expect the registrations to happen positionally", jiraKey = "HHH-18280" )
	public void testNamedParameters() {
		inTransaction( (session) -> {
			final ProcedureCall findUserRange = session.createStoredProcedureCall( "findUserRange" );
			findUserRange.registerParameter( "end", int.class, ParameterMode.IN );
			findUserRange.registerParameter( "start", int.class, ParameterMode.IN );
			findUserRange.setParameter( "start", 1 );
			findUserRange.setParameter( "end", 10 );
			final List<?> resultList = findUserRange.getResultList();
			assertThat( resultList ).hasSize( 9 );
		} );
	}
}
