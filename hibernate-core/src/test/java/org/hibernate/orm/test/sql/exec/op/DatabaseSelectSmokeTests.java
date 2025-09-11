/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.op;

import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.internal.LockTimeoutHandler;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.ops.internal.DatabaseSelectImpl;
import org.hibernate.sql.ops.spi.PostAction;
import org.hibernate.sql.results.spi.SingleResultConsumer;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Smoke tests for {@linkplain org.hibernate.sql.ops.spi.DatabaseSelect}.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = Person.class)
@SessionFactory
public class DatabaseSelectSmokeTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Person( 1, "Steve ") );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testSimpleSelect(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Person.class );

		final Helper.SelectByIdQuery personQuery = Helper.createSelectByIdQuery( entityDescriptor, LockMode.NONE, sessionFactory );
		final JdbcOperationQuerySelect jdbcOperation = personQuery.jdbcOperation();
		final JdbcParameterBindings jdbcParameterBindings = personQuery.jdbcParameterBindings();

		final DatabaseSelectImpl databaseOperation = new DatabaseSelectImpl( jdbcOperation );

		factoryScope.inTransaction( (session) -> {
			final Person person = databaseOperation.execute(
					Person.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					jdbcParameterBindings,
					row -> (Person) row[0],
					SingleResultConsumer.instance(),
					new SingleIdExecutionContext(
							session,
							null,
							1,
							entityDescriptor,
							QueryOptions.NONE,
							null
					)
			);
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsConnectionLockTimeouts.class)
	void testConnectionLockTimeout(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final LockingSupport lockingSupport = sessionFactory.getJdbcServices().getDialect().getLockingSupport();
		final ConnectionLockTimeoutStrategy lockTimeoutStrategy = lockingSupport.getConnectionLockTimeoutStrategy();
		assert lockTimeoutStrategy.getSupportedLevel() != ConnectionLockTimeoutStrategy.Level.NONE;

		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Person.class );

		final Helper.SelectByIdQuery personQuery = Helper.createSelectByIdQuery( entityDescriptor, LockMode.NONE, sessionFactory );
		final JdbcOperationQuerySelect jdbcOperation = personQuery.jdbcOperation();
		final JdbcParameterBindings jdbcParameterBindings = personQuery.jdbcParameterBindings();

		final LockOptions lockOptions = new LockOptions(
				LockMode.PESSIMISTIC_WRITE,
				2000,
				Locking.Scope.INCLUDE_COLLECTIONS,
				Locking.FollowOn.ALLOW
		);

		final LockTimeoutHandler lockTimeoutHandler = new LockTimeoutHandler( lockOptions.getTimeout(), lockTimeoutStrategy );

		final DatabaseSelectImpl databaseOperation = DatabaseSelectImpl.builder( jdbcOperation )
				.addSecondaryActionPair( lockTimeoutHandler )
				.build();

		factoryScope.inTransaction( (session) -> {
			final Person person = databaseOperation.execute(
					Person.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					jdbcParameterBindings,
					row -> (Person) row[0],
					SingleResultConsumer.instance(),
					new SingleIdExecutionContext(
							session,
							null,
							1,
							entityDescriptor,
							QueryOptions.NONE,
							null
					)
			);
		} );
	}

	@Test
	void testFollowOnLockingParadigm(SessionFactoryScope factoryScope) {
		// NOTE: this just tests the principle -
		// for now, just collect the values loaded.
		// ultimately, this can be used to apply smarter follow-on locking.

		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Person.class );

		final Helper.SelectByIdQuery personQuery = Helper.createSelectByIdQuery( entityDescriptor, LockMode.NONE, sessionFactory );
		final JdbcOperationQuerySelect jdbcOperation = personQuery.jdbcOperation();
		final JdbcParameterBindings jdbcParameterBindings = personQuery.jdbcParameterBindings();

		factoryScope.inTransaction( (session) -> {
			final LoadedValueCollector loadedValueCollector = new LoadedValueCollector();

			final Callback callback = new CallbackImpl();
			callback.registerAfterLoadAction( (entity, entityMappingType, session1) ->  {
				loadedValueCollector.loadedValues.add( entity );
			} );

			final SingleIdExecutionContext executionContext = new SingleIdExecutionContext(
					session,
					null,
					1,
					entityDescriptor,
					QueryOptions.NONE,
					callback
			);


			final DatabaseSelectImpl.Builder operationBuilder = DatabaseSelectImpl
					.builder( jdbcOperation )
					.appendPostAction( loadedValueCollector );

			final ConnectionLockTimeoutStrategy lockTimeoutStrategy = session
					.getDialect()
					.getLockingSupport()
					.getConnectionLockTimeoutStrategy();
			if ( lockTimeoutStrategy.getSupportedLevel() != ConnectionLockTimeoutStrategy.Level.NONE ) {
				final LockTimeoutHandler lockTimeoutHandler = new LockTimeoutHandler( Timeout.seconds( 2 ), lockTimeoutStrategy );
				operationBuilder.addSecondaryActionPair( lockTimeoutHandler, lockTimeoutHandler );
			}

			final DatabaseSelectImpl databaseOperation = operationBuilder.build();
			final Person person = databaseOperation.execute(
					Person.class,
					1,
					StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
					jdbcParameterBindings,
					row -> (Person) row[0],
					SingleResultConsumer.instance(),
					executionContext
			);

			assertThat( loadedValueCollector.loadedValues ).hasSize( 1 );
		} );

	}

	private static class LoadedValueCollector implements PostAction {
		private final List<Object> loadedValues = new ArrayList<>();

		@Override
		public void performPostAction(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
			loadedValues.forEach( (value) -> {
				System.out.printf( "Loaded value: %s\n", value );
			} );
		}
	}


}
