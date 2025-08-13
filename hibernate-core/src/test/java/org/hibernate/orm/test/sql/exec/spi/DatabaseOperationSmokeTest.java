/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.spi;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import org.hibernate.ScrollMode;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.internal.DatabaseOperationSelectImpl;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.StatementAccess;
import org.hibernate.sql.results.spi.SingleResultConsumer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = DatabaseOperationSmokeTest.Person.class)
@SessionFactory
public class DatabaseOperationSmokeTest {
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

		final PersonQuery personQuery = createPersonQuery( entityDescriptor, sessionFactory );
		final JdbcOperationQuerySelect jdbcOperation = personQuery.jdbcOperation();
		final JdbcParameterBindings jdbcParameterBindings = personQuery.jdbcParameterBindings();

		final DatabaseOperationSelectImpl databaseOperation = new DatabaseOperationSelectImpl( jdbcOperation );

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
	void testConnectionLockTimeout(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final LockingSupport lockingSupport = sessionFactory.getJdbcServices().getDialect().getLockingSupport();
		final ConnectionLockTimeoutStrategy lockTimeoutStrategy = lockingSupport.getConnectionLockTimeoutStrategy();
		if ( lockTimeoutStrategy.getSupportedLevel() == ConnectionLockTimeoutStrategy.Level.NONE ) {
			return;
		}

		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Person.class );

		final PersonQuery personQuery = createPersonQuery( entityDescriptor, sessionFactory );
		final JdbcOperationQuerySelect jdbcOperation = personQuery.jdbcOperation();
		final JdbcParameterBindings jdbcParameterBindings = personQuery.jdbcParameterBindings();


		final LockTimeoutHandler lockTimeoutHandler = new LockTimeoutHandler( Timeout.seconds( 2 ), lockTimeoutStrategy );

		final DatabaseOperationSelectImpl databaseOperation = DatabaseOperationSelectImpl.builder( jdbcOperation )
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

	private static class LockTimeoutHandler implements PreAction, PostAction {
		private final ConnectionLockTimeoutStrategy lockTimeoutStrategy;
		private final Timeout timeout;
		private Timeout baseline;

		public LockTimeoutHandler(Timeout timeout, ConnectionLockTimeoutStrategy lockTimeoutStrategy) {
			this.timeout = timeout;
			this.lockTimeoutStrategy = lockTimeoutStrategy;
		}

		public Timeout getBaseline() {
			return baseline;
		}

		@Override
		public void performPreAction(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
			final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

			// first, get the baseline (for post-action)
			baseline = lockTimeoutStrategy.getLockTimeout( jdbcConnection, factory );

			// now set the timeout
			lockTimeoutStrategy.setLockTimeout( timeout, jdbcConnection, factory );
		}

		@Override
		public void performPostAction(StatementAccess jdbcStatementAccess, Connection jdbcConnection, ExecutionContext executionContext) {
			final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

			// reset the timeout
			lockTimeoutStrategy.setLockTimeout( baseline, jdbcConnection, factory );
		}
	}


	private PersonQuery createPersonQuery(
			EntityPersister entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		final MutableObject<JdbcParameter> jdbcParamRef = new MutableObject<>();
		final SelectStatement selectAst = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				null,
				entityDescriptor.getIdentifierMapping(),
				null,
				1,
				new LoadQueryInfluencers( sessionFactory ),
				null,
				jdbcParamRef::setIfNot,
				sessionFactory
		);

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( 1 );
		jdbcParameterBindings.addBinding(
				jdbcParamRef.get(),
				new JdbcParameterBindingImpl(
						entityDescriptor.getIdentifierMapping().getJdbcMapping( 0 ),
						1
				)
		);
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcOperationQuerySelect jdbcOperation = jdbcServices
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, selectAst )
				.translate( jdbcParameterBindings, QueryOptions.NONE );

		return new PersonQuery( jdbcOperation, jdbcParameterBindings );
	}

	private record PersonQuery(
			JdbcOperationQuerySelect jdbcOperation,
			JdbcParameterBindings jdbcParameterBindings) {
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	private static class SingleIdExecutionContext extends BaseExecutionContext {
		private final Object entityInstance;
		private final Object restrictedValue;
		private final EntityMappingType rootEntityDescriptor;
		private final QueryOptions queryOptions;
		private final Callback callback;

		public SingleIdExecutionContext(
				SharedSessionContractImplementor session,
				Object entityInstance,
				Object restrictedValue,
				EntityMappingType rootEntityDescriptor, QueryOptions queryOptions,
				Callback callback) {
			super( session );
			this.entityInstance = entityInstance;
			this.restrictedValue = restrictedValue;
			this.rootEntityDescriptor = rootEntityDescriptor;
			this.queryOptions = queryOptions;
			this.callback = callback;
		}

		@Override
		public Object getEntityInstance() {
			return entityInstance;
		}

		@Override
		public Object getEntityId() {
			return restrictedValue;
		}

		@Override
		public EntityMappingType getRootEntityDescriptor() {
			return rootEntityDescriptor;
		}

		@Override
		public QueryOptions getQueryOptions() {
			return queryOptions;
		}

		@Override
		public Callback getCallback() {
			return callback;
		}

	}
}
