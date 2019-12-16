/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.SortOrder;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Represents a column-reference used in an order-by fragment
 *
 * @apiNote This is Hibernate-specific feature.  For {@link javax.persistence.OrderBy} (JPA)
 * all path references are expected to be domain paths (attributes).
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements OrderingExpression, SequencePart {
	private final String columnExpression;
	private final NavigablePath rootPath;

	public ColumnReference(String columnExpression, NavigablePath rootPath) {
		this.columnExpression = columnExpression;
		this.rootPath = rootPath;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			boolean isTerminal,
			TranslationContext translationContext) {
		throw new UnsupportedOperationException( "ColumnReference cannot be de-referenced" );
	}

	@Override
	public void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			SortOrder sortOrder,
			SqlAstCreationState creationState) {
		final TableReference primaryTableReference = tableGroup.getPrimaryTableReference();

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();

		ast.addSortSpecification(
				new SortSpecification(
						sqlExpressionResolver.resolveSqlExpression(
								SqlExpressionResolver.createColumnReferenceKey( primaryTableReference, columnExpression ),
								sqlAstProcessingState -> new org.hibernate.sql.ast.tree.expression.ColumnReference(
										tableGroup.getPrimaryTableReference(),
										columnExpression,
										// because these ordering fragments are only ever part of the order-by clause, there
										//		is no need for the JdbcMapping
										null,
										creationState.getCreationContext().getSessionFactory()
								)
						),
						collation,
						sortOrder
				)
		);
	}
}
