/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.spi.expression.CountFunction;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.MaxFunction;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;

/**
 * @author Chris Cranford
 */
public class CollectionSizeSelector extends AbstractSelector {

	public CollectionSizeSelector(
			PersistentCollectionDescriptor collectionDescriptor,
			Table table,
			String sqlWhereString,
			SessionFactoryImplementor sessionFactory) {
		super( collectionDescriptor, table, sqlWhereString, sessionFactory );
	}

	@SuppressWarnings("unchecked")
	public int execute(Object key, SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
		bindCollectionKey( key, jdbcParameterBindings, session );

		List results = execute( jdbcParameterBindings, session );

		if ( results.size() != 1 ) {
			return 0;
		}

		Object result = results.get( 0 );
		if ( result == null ) {
			return 0;
		}

		if ( isIntegerIndexed() ) {
			return (int) result + 1;
		}
		else {
			return (int) result;
		}
	}


	protected void applySqlSelections(
			TableGroup tableGroup,
			SelectClause selectClause,
			Consumer<DomainResult> domainResultsCollector,
			SessionFactoryImplementor sessionFactory) {
		if ( isIntegerIndexed() ) {
			// Build selection of "max(indexColumn[0])"
			final List<Column> indexColumns = getCollectionDescriptor().getIndexDescriptor().getColumns();
			final Column column = indexColumns.get( 0 );

			final Expression columnExpression = tableGroup.qualify( column );

			final Expression maxExpression = new MaxFunction(
					columnExpression,
					false,
					column.getExpressableType()
			);

			SqlSelection sqlSelection = new SqlSelectionImpl(
					1,
					0,
					maxExpression,
					column.getExpressableType().getJdbcValueExtractor()
			);

			selectClause.addSqlSelection( sqlSelection );

			domainResultsCollector.accept(
					new BasicResultImpl(
							null,
							sqlSelection,
							indexColumns.get( 0 ).getExpressableType()
					)
			);
		}
		else {
			// Build selection of "count(1)"
			final SqlExpressableType sqlExpressableType = IntegerSqlDescriptor.INSTANCE.getSqlExpressableType(
					IntegerJavaDescriptor.INSTANCE,
					sessionFactory.getTypeConfiguration()
			);

			final Expression countExpression = new CountFunction(
					new QueryLiteral(
							1,
							sqlExpressableType,
							Clause.SELECT
					),
					false,
					sqlExpressableType
			);

			SqlSelection sqlSelection = new SqlSelectionImpl(
					1,
					0,
					countExpression,
					sqlExpressableType.getJdbcValueExtractor()
			);

			selectClause.addSqlSelection( sqlSelection );

			domainResultsCollector.accept(
					new BasicResultImpl(
							null,
							sqlSelection,
							sqlExpressableType
					)
			);
		}
	}

	@Override
	protected void applyPredicates(
			Junction junction,
			TableGroup tableGroup,
			Consumer<JdbcParameterBinder> jdbcParameterBinder,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {
		applyPredicates(
				junction,
				getCollectionDescriptor().getCollectionKeyDescriptor(),
				tableGroup,
				jdbcParameterBinder,
				columnCollector,
				sessionFactory
		);
	}


	private boolean isIntegerIndexed() {
		return getCollectionDescriptor().getIndexDescriptor() != null && getCollectionDescriptor().getCollectionClassification() != CollectionClassification.MAP;
	}
}
