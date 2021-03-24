/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.query.SortOrder;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Represents a column-reference used in an order-by fragment
 *
 * @author Steve Ebersole
 * @apiNote This is Hibernate-specific feature.  For {@link javax.persistence.OrderBy} (JPA)
 * all path references are expected to be domain paths (attributes).
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements OrderingExpression, SequencePart {
	private final String columnExpression;
	private final boolean isColumnExpressionFormula;
	private final NavigablePath rootPath;

	public ColumnReference(String columnExpression, boolean isColumnExpressionFormula, NavigablePath rootPath) {
		this.columnExpression = columnExpression;
		this.isColumnExpressionFormula = isColumnExpressionFormula;
		this.rootPath = rootPath;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	public boolean isColumnExpressionFormula() {
		return isColumnExpressionFormula;
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
			String modelPartName,
			SortOrder sortOrder,
			SqlAstCreationState creationState) {
		TableReference tableReference;

		tableReference = getTableReference( tableGroup );

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();
		final Expression expression = sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( tableReference, columnExpression ),
				sqlAstProcessingState -> new org.hibernate.sql.ast.tree.expression.ColumnReference(
						tableReference,
						columnExpression,
						isColumnExpressionFormula,
						// because these ordering fragments are only ever part of the order-by clause, there
						//		is no need for the JdbcMapping
						null,
						null,
						null,
						creationState.getCreationContext().getSessionFactory()
				)
		);
		// It makes no sense to order by an expression multiple times
		// SQL Server even reports a query error in this case
		if ( ast.hasSortSpecifications() ) {
			for ( SortSpecification sortSpecification : ast.getSortSpecifications() ) {
				if ( sortSpecification.getSortExpression() == expression ) {
					return;
				}
			}
		}

		ast.addSortSpecification( new SortSpecification( expression, collation, sortOrder ) );
	}

	TableReference getTableReference(TableGroup tableGroup) {
		ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof PluralAttributeMapping ) {
			MappingType partMappingType = ( (PluralAttributeMapping) modelPart ).getElementDescriptor()
					.getPartMappingType();
			if ( partMappingType instanceof AbstractEntityPersister ) {
				AbstractEntityPersister abstractEntityPersister = (AbstractEntityPersister) partMappingType;
				int i = abstractEntityPersister.determineTableNumberForColumn( columnExpression );
				String tableName = abstractEntityPersister.getTableName( i );
				for ( TableReferenceJoin tableReferenceJoin : tableGroup.getTableReferenceJoins() ) {
					final TableReference joinedTableReference = tableReferenceJoin.getJoinedTableReference();
					if ( joinedTableReference.getTableExpression()
							.equals( tableName ) ) {
						return joinedTableReference;
					}
				}
			}
			else {
				return tableGroup.getPrimaryTableReference();
			}
		}
		return null;
	}

}
