/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class JoinTableRemovalExecutor implements CollectionRemovalExecutor {
	private final PersistentCollectionDescriptor collectionDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	private final Map<Column, JdbcParameter> jdbcParameterMap;
	private final JdbcDelete removalOperation;

	public JoinTableRemovalExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this.collectionDescriptor = collectionDescriptor;
		this.sessionFactory = sessionFactory;

		final TableReference collectionTableRef = new TableReference(
				collectionDescriptor.getSeparateCollectionTable(),
				null,
				false
		);

		this.jdbcParameterMap = new HashMap<>();

		final DeleteStatement deleteStatement = generateDeleteStatement(
				collectionTableRef,
				jdbcParameterMap::put,
				collectionDescriptor,
				sessionFactory
		);

		this.removalOperation = SqlDeleteToJdbcDeleteConverter.interpret(
				deleteStatement,
				sessionFactory
		);
	}

	private DeleteStatement generateDeleteStatement(
			TableReference collectionTableRef,
			BiConsumer<Column, JdbcParameter> parameterCollector,
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory) {

		final AtomicInteger parameterCount = new AtomicInteger(  );

		final Junction deleteRestriction = new Junction( Junction.Nature.CONJUNCTION );
		collectionDescriptor.getCollectionKeyDescriptor().visitColumns(
				(BiConsumer<SqlExpressableType, Column>) (jdbcType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.INSERT,
							sessionFactory.getTypeConfiguration()
					);

					parameterCollector.accept( column, parameter );

					deleteRestriction.add(
							new ComparisonPredicate(
									collectionTableRef.qualify( column ), ComparisonOperator.EQUAL,
									parameter
							)
					);
				},
				Clause.DELETE,
				sessionFactory.getTypeConfiguration()
		);

		return new DeleteStatement( collectionTableRef, deleteRestriction );
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
				Clause.INSERT,
				session
		);

		JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( removalOperation, executionContext );
	}

	@SuppressWarnings("WeakerAccess")
	protected void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType type,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final JdbcParameter jdbcParameter = resolveJdbcParameter( boundColumn );

		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new LiteralParameter(
						jdbcValue,
						type,
						Clause.INSERT,
						session.getFactory().getTypeConfiguration()
				)
		);
	}

	private JdbcParameter resolveJdbcParameter(Column boundColumn) {
		final JdbcParameter jdbcParameter = jdbcParameterMap.get( boundColumn );
		if ( jdbcParameter == null ) {
			throw new IllegalStateException( "JdbcParameter not found for Column [" + boundColumn + "]" );
		}
		return jdbcParameter;
	}
}
