/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class OneToManyRemovalExecutor implements CollectionRemovalExecutor {
	private final PersistentCollectionDescriptor collectionDescriptor;

	private final Map<Column, JdbcParameter> jdbcParametersMap;
	private final JdbcMutation updateMutation;

	public OneToManyRemovalExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			Table dmlTargetTable,
			SessionFactoryImplementor sessionFactory) {
		this.collectionDescriptor = collectionDescriptor;
		this.jdbcParametersMap = new HashMap<>();

		final UpdateStatement updateStatement = generateUpdateStatement(
				new TableReference( dmlTargetTable, null, false ),
				jdbcParametersMap::put,
				collectionDescriptor,
				sessionFactory
		);

		this.updateMutation = UpdateToJdbcUpdateConverter.createJdbcUpdate(
				updateStatement,
				sessionFactory
		);
	}

	@Override
	public void execute(Object key, SharedSessionContractImplementor session) {
		final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
		final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

		collectionDescriptor.getCollectionKeyDescriptor().dehydrate(
				collectionDescriptor.getCollectionKeyDescriptor().unresolve( key, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.WHERE,
				session
		);

		JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( updateMutation, executionContext );
	}

	private void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType type,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final JdbcParameter jdbcParameter = resolveJdbcParmeter( boundColumn );
		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new LiteralParameter(
						jdbcValue,
						type,
						Clause.UPDATE,
						session.getFactory().getTypeConfiguration()
				)
		);
	}

	private JdbcParameter resolveJdbcParmeter(Column boundColumn) {
		final JdbcParameter jdbcParameter = jdbcParametersMap.get( boundColumn );
		if ( jdbcParameter == null ) {
			throw new IllegalStateException( "JdbcParameter not found for Column [" + boundColumn + "]" );
		}
		return jdbcParameter;
	}

	private UpdateStatement generateUpdateStatement(
			TableReference dmlTableRef,
			BiConsumer<Column, JdbcParameter> columnConsumer,
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory) {

		final List<Assignment> assignments = new ArrayList<>();
		final AtomicInteger parameterCount = new AtomicInteger();

		// Bind null value for collection key columns.
		final Navigable<?> collectionKey = collectionDescriptor.getCollectionKeyDescriptor();
		collectionKey.visitColumns(
				(sqlExpressableType, column) -> {
					final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

					final LiteralParameter parameter = new LiteralParameter(
							null,
							column.getExpressableType(),
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);

					final Assignment assignment = new Assignment( columnReference, parameter );
					assignments.add( assignment );
				},
				Clause.UPDATE,
				sessionFactory.getTypeConfiguration()
		);

		if ( collectionDescriptor.getIndexDescriptor() != null ) {
			final CollectionIndex<?> collectionIndex = collectionDescriptor.getIndexDescriptor();
			collectionIndex.visitColumns(
					(sqlExpressableType, column) -> {
						if ( column.isUpdatable() ) {
							final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

							final LiteralParameter parameter = new LiteralParameter(
									null,
									column.getExpressableType(),
									Clause.UPDATE,
									sessionFactory.getTypeConfiguration()
							);

							final Assignment assignment = new Assignment( columnReference, parameter );
							assignments.add( assignment );
						}
					},
					Clause.UPDATE,
					sessionFactory.getTypeConfiguration()
			);
		}

		// Build parameterized predicate clause.
		Junction junction = new Junction( Junction.Nature.CONJUNCTION );
		collectionKey.visitColumns(
				(sqlExpressableType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);

					columnConsumer.accept( column, parameter );

					junction.add(
							new ComparisonPredicate(
									new ColumnReference( column ),
									ComparisonOperator.EQUAL,
									parameter
							)
					);
				},
				Clause.UPDATE,
				sessionFactory.getTypeConfiguration()
		);

		return new UpdateStatement( dmlTableRef, assignments, junction );
	}
}
