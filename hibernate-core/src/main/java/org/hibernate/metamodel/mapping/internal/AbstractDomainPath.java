/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ordering.ast.DomainPath;
import org.hibernate.metamodel.mapping.ordering.ast.OrderingExpression;
import org.hibernate.query.SortDirection;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractDomainPath implements DomainPath {
	public static final String ELEMENT_TOKEN = "$element$";

	@Override
	public SqlAstNode resolve(
			QuerySpec ast,
			TableGroup tableGroup,
			String modelPartName,
			SqlAstCreationState creationState) {
		return resolve(
				getReferenceModelPart(),
				ast,
				tableGroup,
				modelPartName,
				creationState
		);
	}

	public Expression resolve(
			ModelPart referenceModelPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String modelPartName,
			SqlAstCreationState creationState) {
		final BasicValuedModelPart selection = referenceModelPart.asBasicValuedModelPart();
		if ( selection != null ) {
			final TableReference tableReference = tableGroup.resolveTableReference(
					null,
					selection,
					selection.getContainingTableExpression()
			);
			return creationState.getSqlExpressionResolver().resolveSqlExpression(
					createColumnReferenceKey(
							tableReference,
							selection.getSelectionExpression(),
							selection.getJdbcMapping()
					),
					processingState -> new ColumnReference(
							tableReference,
							selection
					)
			);
		}
		else if ( referenceModelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			final ModelPart subPart =
					ELEMENT_TOKEN.equals( modelPartName )
							? entityValuedModelPart.getEntityMappingType().getIdentifierMapping()
							: entityValuedModelPart.findSubPart( modelPartName );
			return resolve( subPart, ast, tableGroup, modelPartName, creationState );
		}
		else if ( referenceModelPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			if ( embeddableValuedModelPart.getFetchableName()
					.equals( modelPartName ) || ELEMENT_TOKEN.equals( modelPartName ) ) {
				final int size = embeddableValuedModelPart.getNumberOfFetchables();
				final List<Expression> expressions = new ArrayList<>( size );
				for ( int i = 0; i < size; i++ ) {
					final Fetchable fetchable = embeddableValuedModelPart.getFetchable( i );
					expressions.add( resolve( fetchable, ast, tableGroup, modelPartName, creationState ) );
				}
				return new SqlTuple( expressions, embeddableValuedModelPart );
			}
			else {
				ModelPart subPart = embeddableValuedModelPart.findSubPart( modelPartName, null );
				assert subPart.asBasicValuedModelPart() != null;
				return resolve( subPart, ast, tableGroup, modelPartName, creationState );
			}
		}
		else {
			// sure it can happen
			throw new UnsupportedOperationException( "Ordering for " + referenceModelPart + " not supported" );
		}
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
		apply(
				getReferenceModelPart(),
				ast,
				tableGroup,
				collation,
				modelPartName,
				sortOrder,
				nullPrecedence,
				creationState
		);
	}

	private void apply(
			ModelPart referenceModelPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			SqlAstCreationState creationState) {
		final BasicValuedModelPart basicPart = referenceModelPart.asBasicValuedModelPart();
		if ( basicPart != null ) {
			addSortSpecification(
					basicPart,
					ast,
					tableGroup,
					collation,
					sortOrder,
					nullPrecedence,
					creationState
			);
		}
		else if ( referenceModelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			final ModelPart subPart = ELEMENT_TOKEN.equals( modelPartName )
					? entityValuedModelPart.getEntityMappingType().getIdentifierMapping()
					// Default to using the foreign key of an entity valued model part
					: entityValuedModelPart.findSubPart( ForeignKeyDescriptor.PART_NAME );
			apply(
					subPart,
					ast,
					tableGroup,
					collation,
					modelPartName,
					sortOrder,
					nullPrecedence,
					creationState
			);
		}
		else if ( referenceModelPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			addSortSpecification(
					embeddableValuedModelPart,
					ast,
					tableGroup,
					collation,
					modelPartName,
					sortOrder,
					nullPrecedence,
					creationState
			);
		}
		else {
			// sure it can happen
			throw new UnsupportedOperationException( "Ordering for " + getReferenceModelPart() + " not supported" );
		}
	}

	private void addSortSpecification(
			EmbeddableValuedModelPart embeddableValuedModelPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			SqlAstCreationState creationState) {
		if ( embeddableValuedModelPart.getFetchableName()
				.equals( modelPartName ) || ELEMENT_TOKEN.equals( modelPartName ) ) {
			embeddableValuedModelPart.forEachSelectable(
					(columnIndex, selection) -> {
						addSortSpecification(
								selection,
								ast,
								tableGroup,
								collation,
								sortOrder,
								nullPrecedence,
								creationState
						);
					}
			);
		}
		else {
			ModelPart subPart = embeddableValuedModelPart.findSubPart( modelPartName, null );
			addSortSpecification(
					castNonNull( subPart.asBasicValuedModelPart() ),
					ast,
					tableGroup,
					collation,
					sortOrder,
					nullPrecedence,
					creationState
			);
		}
	}

	private void addSortSpecification(
			SelectableMapping selection,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			SqlAstCreationState creationState) {
		final TableReference tableReference = tableGroup.resolveTableReference( null, selection.getContainingTableExpression() );
		final Expression expression = creationState.getSqlExpressionResolver().resolveSqlExpression(
				createColumnReferenceKey(
						tableReference,
						selection.getSelectionExpression(),
						selection.getJdbcMapping()
				),
				processingState -> new ColumnReference(
						tableReference,
						selection
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

		final SelectClause selectClause = ast.getSelectClause();

		if ( selectClause.isDistinct() && selectClauseDoesNotContainOrderExpression( expression, selectClause ) ) {
			final int valuesArrayPosition = selectClause.getSqlSelections().size();
			SqlSelection sqlSelection = new SqlSelectionImpl(
					valuesArrayPosition,
					expression
			);
			selectClause.addSqlSelection( sqlSelection );
		}

		final Expression sortExpression = OrderingExpression.applyCollation(
				expression,
				collation,
				creationState
		);
		ast.addSortSpecification( new SortSpecification( sortExpression, sortOrder, nullPrecedence ) );
	}

	private static boolean selectClauseDoesNotContainOrderExpression(Expression expression, SelectClause selectClause) {
		for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
			if ( sqlSelection.getExpression().equals( expression ) ) {
				return false;
			}
		}
		return true;
	}
}
