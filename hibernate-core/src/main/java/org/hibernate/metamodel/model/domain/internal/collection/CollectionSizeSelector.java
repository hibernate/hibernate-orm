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
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SelfRenderingExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

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

	@Override
	protected void applySqlSelections(
			QuerySpec querySpec,
			TableGroup tableGroup,
			SelectClause selectClause,
			Consumer<DomainResult> domainResultsCollector,
			DomainResultCreationState creationState) {
		if ( isIntegerIndexed() ) {
			// Build selection of "max(indexColumn[0])"
			final List<Column> indexColumns = getCollectionDescriptor().getIndexDescriptor().getColumns();
			final Column column = indexColumns.get( 0 );

			final Expression columnExpression = tableGroup.qualify( column );

			final Expression maxExpression = new SelfRenderingExpression() {
				@Override
				public void renderToSql(SqlAppender sqlAppender, SqlAstWalker walker, SessionFactoryImplementor sessionFactory) {
					sqlAppender.appendSql("max(");
					columnExpression.accept(walker);
					sqlAppender.appendSql(")");
				}
				@Override
				public SqlExpressableType getType() {
					return column.getExpressableType();
				}
				@Override
				public SqlSelection createSqlSelection(int jdbcPosition, int valuesArrayPosition, BasicJavaDescriptor javaTypeDescriptor, TypeConfiguration typeConfiguration) {
					return null;
				}
			};

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
					creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
			);

			final Expression countExpression = new SelfRenderingExpression() {
				@Override
				public void renderToSql(SqlAppender sqlAppender, SqlAstWalker walker, SessionFactoryImplementor sessionFactory) {
					sqlAppender.appendSql("count(");
					new QueryLiteral(
							1,
							sqlExpressableType,
							Clause.SELECT
					).accept(walker);
					sqlAppender.appendSql(")");
				}
				@Override
				public SqlExpressableType getType() {
					return sqlExpressableType;
				}
				@Override
				public SqlSelection createSqlSelection(int jdbcPosition, int valuesArrayPosition, BasicJavaDescriptor javaTypeDescriptor, TypeConfiguration typeConfiguration) {
					return null;
				}
			};

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
			BiConsumer<Column, JdbcParameter> columnCollector,
			SqlAstCreationState creationState) {
		applyPredicates(
				junction,
				getCollectionDescriptor().getCollectionKeyDescriptor(),
				tableGroup,
				columnCollector,
				creationState
		);
	}


	private boolean isIntegerIndexed() {
		return getCollectionDescriptor().getIndexDescriptor() != null && getCollectionDescriptor().getCollectionClassification() != CollectionClassification.MAP;
	}
}
