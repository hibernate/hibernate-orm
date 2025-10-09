/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.internal;

import java.sql.Statement;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueBindingsImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.orm.test.common.JournalingBatchObserver;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
@SessionFactory
public class BatchingTest implements BatchKey {
	private final String SANDBOX_TBL = "SANDBOX_JDBC_TST";

	@Test
	public void testBatchingUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final var jdbcCoordinator = session.getJdbcCoordinator();
			final var insertSql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";

			final var batchBuilder = new BatchBuilderImpl( 2 );
			final var batchKey = new BasicBatchKey( "this" );
			final var insertBatch = batchBuilder.buildBatch( batchKey, null, SANDBOX_TBL, session, insertSql );
			assertThat( insertBatch ).isNotNull();

			final var batchObserver = new JournalingBatchObserver();
			insertBatch.addObserver( batchObserver );

			final var jdbcValueBindings = sandboxInsertValueBindings( session );

			// bind values for #1 - should do nothing at the JDBC level
			jdbcValueBindings.bindValue( 1, SANDBOX_TBL, "ID", ParameterUsage.SET );
			jdbcValueBindings.bindValue( "name", SANDBOX_TBL, "NAME", ParameterUsage.SET );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();

			// add #1 to the batch - will acquire prepared statement to bind values
			insertBatch.addToBatch( jdbcValueBindings, null );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isTrue();

			// bind values for #2 - again, nothing at JDBC level (we have statement from earlier)
			jdbcValueBindings.bindValue( 2, SANDBOX_TBL, "ID", ParameterUsage.SET );
			jdbcValueBindings.bindValue( "another name", SANDBOX_TBL, "NAME", ParameterUsage.SET );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );

			// add #2 to the batch -
			// 		- uses the previous prepared statement to bind values
			//		- batch size has been exceeded, trigger an implicit execution
			insertBatch.addToBatch( jdbcValueBindings, null );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 1 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isTrue();

			// execute the batch - effectively only increments the explicit-execution counter
			insertBatch.execute();
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 1 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 1 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();

			insertBatch.release();
		} );
	}

	private JdbcValueBindingsImpl sandboxInsertValueBindings(SessionImplementor session) {
		return new JdbcValueBindingsImpl(
				MutationType.INSERT,
				null,
				new JdbcValueBindingsImpl.JdbcValueDescriptorAccess() {
					@Override
					public JdbcValueDescriptor resolveValueDescriptor(
							String tableName,
							String columnName,
							ParameterUsage usage) {
						assert tableName.equals( SANDBOX_TBL );

						if ( columnName.equals( "ID" ) ) {
							return new JdbcValueDescriptor() {
								@Override
								public String getColumnName() {
									return "ID";
								}

								@Override
								public ParameterUsage getUsage() {
									return ParameterUsage.SET;
								}

								@Override
								public int getJdbcPosition() {
									return 1;
								}

								@Override
								public JdbcMapping getJdbcMapping() {
									return session.getTypeConfiguration()
											.getBasicTypeRegistry()
											.resolve( StandardBasicTypes.INTEGER );
								}
							};
						}

						if ( columnName.equals( "NAME" ) ) {
							return new JdbcValueDescriptor() {
								@Override
								public String getColumnName() {
									return "NAME";
								}

								@Override
								public ParameterUsage getUsage() {
									return ParameterUsage.SET;
								}

								@Override
								public int getJdbcPosition() {
									return 2;
								}

								@Override
								public JdbcMapping getJdbcMapping() {
									return session.getTypeConfiguration()
											.getBasicTypeRegistry()
											.resolve( StandardBasicTypes.STRING );
								}
							};
						}

						throw new IllegalArgumentException( "Unknown column : " + columnName );
					}
				},
				session
		);
	}

	@Test
	public void testSessionBatchingUsage(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var jdbcCoordinator = session.getJdbcCoordinator();

			session.setJdbcBatchSize( 3 );

			final String insertSql = "insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )";

			final BatchBuilderImpl batchBuilder = new BatchBuilderImpl( 2 );
			final BatchKey batchKey = new BasicBatchKey( "this" );
			final Batch insertBatch = batchBuilder.buildBatch( batchKey, 3, SANDBOX_TBL, session, insertSql );
			assertThat( insertBatch ).isNotNull();

			final JournalingBatchObserver batchObserver = new JournalingBatchObserver();
			insertBatch.addObserver( batchObserver );

			final JdbcValueBindingsImpl jdbcValueBindings = sandboxInsertValueBindings( session );

			// bind values for #1 - this does nothing at the JDBC level
			jdbcValueBindings.bindValue( 1, SANDBOX_TBL, "ID", ParameterUsage.SET );
			jdbcValueBindings.bindValue( "name", SANDBOX_TBL, "NAME", ParameterUsage.SET );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();

			// add the values to the batch - this creates the prepared statement and binds the values
			insertBatch.addToBatch( jdbcValueBindings, null );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isTrue();

			// bind values for #2 - this does nothing at the JDBC level : we do still have the statement defining the batch
			jdbcValueBindings.bindValue( 2, SANDBOX_TBL, "ID", ParameterUsage.SET );
			jdbcValueBindings.bindValue( "another name", SANDBOX_TBL, "NAME", ParameterUsage.SET );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isTrue();

			// add #2 to batch - we have not exceeded batch size, so we should not get an implicit execution
			insertBatch.addToBatch( jdbcValueBindings, null );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isTrue();

			// bind values for #3 - this does nothing at the JDBC level : we do still have the statement defining the batch
			jdbcValueBindings.bindValue( 3, SANDBOX_TBL, "ID", ParameterUsage.SET );
			jdbcValueBindings.bindValue( "yet another name", SANDBOX_TBL, "NAME", ParameterUsage.SET );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isTrue();

			insertBatch.addToBatch( jdbcValueBindings, null );
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 0 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 1 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isTrue();

			insertBatch.execute();
			assertThat( batchObserver.getExplicitExecutionCount() ).isEqualTo( 1 );
			assertThat( batchObserver.getImplicitExecutionCount() ).isEqualTo( 1 );
			assertThat( jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() ).isFalse();

			insertBatch.release();

		} );
	}

	@BeforeEach
	void exportSandboxSchema(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var jdbcCoordinator = session.getJdbcCoordinator();
			var logicalConnection = jdbcCoordinator.getLogicalConnection();
			var dialect = session.getDialect();

			// set up some tables to use
			Statement statement = jdbcCoordinator.getStatementPreparer().createStatement();
			String dropSql = dialect.getDropTableString( "SANDBOX_JDBC_TST" );
			try {
				jdbcCoordinator.getResultSetReturn().execute( statement, dropSql );
			}
			catch ( Exception e ) {
				// ignore if the DB doesn't support "if exists" and the table doesn't exist
			}

			jdbcCoordinator.getResultSetReturn().execute( statement, "create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
			Assertions.assertTrue(
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() );
			Assertions.assertTrue( logicalConnection.isPhysicallyConnected() );

			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( statement );
			Assertions.assertFalse(
					jdbcCoordinator.getLogicalConnection().getResourceRegistry().hasRegisteredResources() );
			Assertions.assertTrue( logicalConnection.isPhysicallyConnected() ); // after_transaction specified
		} );
	}

	@AfterEach
	void cleanupTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( connection -> {
			try ( Statement stmnt = connection.createStatement() ) {
				stmnt.execute( session.getDialect().getDropTableString( "SANDBOX_JDBC_TST" ) );
			}
		} ) );
	}

}
