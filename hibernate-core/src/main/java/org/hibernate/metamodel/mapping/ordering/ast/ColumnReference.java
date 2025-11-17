/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SortDirection;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.type.NullType;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * Represents a column-reference used in an order-by fragment
 *
 * @apiNote This is Hibernate-specific feature.  For {@link jakarta.persistence.OrderBy} (JPA)
 * all path references are expected to be domain paths (attributes).
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements OrderingExpression, SequencePart {
	private final String columnExpression;
	private final boolean isColumnExpressionFormula;

	public ColumnReference(String columnExpression, boolean isColumnExpressionFormula) {
		this.columnExpression = columnExpression;
		this.isColumnExpressionFormula = isColumnExpressionFormula;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	public boolean isColumnExpressionFormula() {
		return isColumnExpressionFormula;
	}

	@Override
	public Expression resolve(
			QuerySpec ast,
			TableGroup tableGroup,
			String modelPartName,
			SqlAstCreationState creationState) {
		final TableReference tableReference = getTableReference( tableGroup );
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();
		return sqlExpressionResolver.resolveSqlExpression(
				createColumnReferenceKey( tableReference, columnExpression, NullType.INSTANCE ),
				sqlAstProcessingState -> new org.hibernate.sql.ast.tree.expression.ColumnReference(
						tableReference,
						columnExpression,
						isColumnExpressionFormula,
						// because these ordering fragments are only ever part of the order-by clause, there
						//		is no need for the JdbcMapping
						null,
						null
				)
		);
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext) {
		throw new UnsupportedMappingException( "ColumnReference cannot be de-referenced" );
	}

	@Override
	public void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			SqlAstCreationState creationState) {
		final Expression expression = resolve( ast, tableGroup, modelPartName, creationState );
		// It makes no sense to order by an expression multiple times
		// SQL Server even reports a query error in this case
		if ( ast.hasSortSpecifications() ) {
			for ( SortSpecification sortSpecification : ast.getSortSpecifications() ) {
				if ( sortSpecification.getSortExpression() == expression ) {
					return;
				}
			}
		}

		final Expression sortExpression =
				OrderingExpression.applyCollation( expression, collation, creationState );
		ast.addSortSpecification( new SortSpecification( sortExpression, sortOrder, nullPrecedence ) );
	}

	TableReference getTableReference(TableGroup tableGroup) {
		ModelPartContainer modelPart = tableGroup.getModelPart();
		if ( modelPart instanceof PluralAttributeMapping pluralAttribute ) {
			if ( !pluralAttribute.getCollectionDescriptor().hasManyToManyOrdering() ) {
				return tableGroup.getPrimaryTableReference();
			}

			if ( pluralAttribute.getElementDescriptor().getPartMappingType()
					instanceof EntityPersister entityPersister ) {
				final String tableName = entityPersister.getTableNameForColumn( columnExpression );
				return tableGroup.getTableReference( tableGroup.getNavigablePath(), tableName );
			}
			else {
				return tableGroup.getPrimaryTableReference();
			}
		}
		return null;
	}

	@Override
	public String toDescriptiveText() {
		return "column reference (" + columnExpression + ")";
	}
}
