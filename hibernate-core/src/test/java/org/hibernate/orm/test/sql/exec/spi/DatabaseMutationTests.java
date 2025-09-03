/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.spi;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcOperationQueryInsertImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQueryUpdate;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.ops.internal.DatabaseOperationMutationImpl;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = Person.class)
@SessionFactory(useCollectingStatementInspector = true)
public class DatabaseMutationTests {
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
	void testSimpleInsert(SessionFactoryScope factoryScope) {
		final String sql = "insert into persons (name,id) values (?,?)";

		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Person.class );

		final MutatingTableReference tableReference = new MutatingTableReference( entityDescriptor.getIdentifierTableMapping() );
		final JdbcParameterBindings paramBindings = new JdbcParameterBindingsImpl( 2 );
		final List<JdbcParameterBinder> paramBinders = new ArrayList<>();

		final SelectableMapping nameColumn = entityDescriptor.getAttributeMapping( 0 ).getSelectable( 0 );
		final ColumnValueParameter nameParam = new ColumnValueParameter(
				new ColumnReference( tableReference, nameColumn ),
				ParameterUsage.SET
		);
		paramBindings.addBinding( nameParam, new JdbcParameterBindingImpl( nameColumn.getJdbcMapping(), "Jacob" ) );
		paramBinders.add( nameParam );

		final SelectableMapping idColumn = entityDescriptor.getIdentifierMapping().getSelectable( 0 );
		final ColumnValueParameter idParam = new ColumnValueParameter(
				new ColumnReference( tableReference, idColumn ),
				ParameterUsage.SET
		);
		paramBindings.addBinding( idParam, new JdbcParameterBindingImpl( idColumn.getJdbcMapping(), 2 ) );
		paramBinders.add( idParam );

		final DatabaseOperationMutationImpl databaseOp = new DatabaseOperationMutationImpl(
				new JdbcOperationQueryInsertImpl(
						sql,
						paramBinders,
						Set.of( "persons" )
				)
		);


		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final int count = databaseOp.execute(
					sqlToPrepare -> session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sqlToPrepare ),
					paramBindings,
					(rowsAffected, preparedStatement) ->
							assertThat( rowsAffected ).isEqualTo( 1 ),
					new ExecutionContextImpl( session )
			);
			assertThat( count ).isEqualTo( 1 );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).isEqualTo( sql );
		} );
	}

	@Test
	void testSimpleUpdate(SessionFactoryScope factoryScope) {
		final String sql = "update persons set name=? where id=?";

		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Person.class );

		final MutatingTableReference tableReference = new MutatingTableReference( entityDescriptor.getIdentifierTableMapping() );
		final JdbcParameterBindingsImpl paramBindings = new JdbcParameterBindingsImpl( 2 );
		final List<JdbcParameterBinder> paramBinders = new ArrayList<>();

		final SelectableMapping nameColumn = entityDescriptor.getAttributeMapping( 0 ).getSelectable( 0 );
		final ColumnValueParameter nameParam = new ColumnValueParameter(
				new ColumnReference( tableReference, nameColumn ),
				ParameterUsage.SET
		);
		paramBindings.addBinding( nameParam, new JdbcParameterBindingImpl( nameColumn.getJdbcMapping(), "Jacob" ) );
		paramBinders.add( nameParam );

		final SelectableMapping idColumn = entityDescriptor.getIdentifierMapping().getSelectable( 0 );
		final ColumnValueParameter idParam = new ColumnValueParameter(
				new ColumnReference( tableReference, idColumn ),
				ParameterUsage.RESTRICT
		);
		paramBindings.addBinding( idParam, new JdbcParameterBindingImpl( idColumn.getJdbcMapping(), 1 ) );
		paramBinders.add( idParam );


		final DatabaseOperationMutationImpl databaseOp = new DatabaseOperationMutationImpl(
				new JdbcOperationQueryUpdate(
						sql,
						paramBinders,
						Set.of( "persons" ),
						paramBindings.getBindingMap()
				)
		);


		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final int count = databaseOp.execute(
					sqlToPrepare -> session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sqlToPrepare ),
					paramBindings,
					(rowsAffected, preparedStatement) ->
							assertThat( rowsAffected ).isEqualTo( 1 ),
					new ExecutionContextImpl( session )
			);
			assertThat( count ).isEqualTo( 1 );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).isEqualTo( sql );
		} );
	}

	@Test
	public void testSimpleDelete(SessionFactoryScope factoryScope) {
		final String sql = "delete persons where id=?";

		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getMappingMetamodel().findEntityDescriptor( Person.class );

		final MutatingTableReference tableReference = new MutatingTableReference( entityDescriptor.getIdentifierTableMapping() );
		final JdbcParameterBindingsImpl paramBindings = new JdbcParameterBindingsImpl( 2 );
		final List<JdbcParameterBinder> paramBinders = new ArrayList<>();

		final SelectableMapping idColumn = entityDescriptor.getIdentifierMapping().getSelectable( 0 );
		final ColumnValueParameter idParam = new ColumnValueParameter(
				new ColumnReference( tableReference, idColumn ),
				ParameterUsage.RESTRICT
		);
		paramBindings.addBinding( idParam, new JdbcParameterBindingImpl( idColumn.getJdbcMapping(), 1 ) );
		paramBinders.add( idParam );


		final DatabaseOperationMutationImpl databaseOp = new DatabaseOperationMutationImpl(
				new JdbcOperationQueryUpdate(
						sql,
						paramBinders,
						Set.of( "persons" ),
						paramBindings.getBindingMap()
				)
		);


		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final int count = databaseOp.execute(
					sqlToPrepare -> session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sqlToPrepare ),
					paramBindings,
					(rowsAffected, preparedStatement) ->
							assertThat( rowsAffected ).isEqualTo( 1 ),
					new ExecutionContextImpl( session )
			);
			assertThat( count ).isEqualTo( 1 );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).isEqualTo( sql );
		} );
	}

	private static class ExecutionContextImpl extends BaseExecutionContext {
		public ExecutionContextImpl(SharedSessionContractImplementor session) {
			super( session );
		}
	}
}
