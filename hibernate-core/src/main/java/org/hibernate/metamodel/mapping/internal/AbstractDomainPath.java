/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortOrder;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ordering.ast.DomainPath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;

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
		if ( referenceModelPart instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart selection = (BasicValuedModelPart) referenceModelPart;
			final TableReference tableReference = tableGroup.resolveTableReference( getNavigablePath(), selection.getContainingTableExpression() );
			return creationState.getSqlExpressionResolver().resolveSqlExpression(
					SqlExpressionResolver.createColumnReferenceKey(
							selection.getContainingTableExpression(),
							selection.getSelectionExpression()
					),
					sqlAstProcessingState -> new ColumnReference(
							tableReference,
							selection,
							creationState.getCreationContext().getSessionFactory()
					)
			);
		}
		else if ( referenceModelPart instanceof EntityValuedModelPart ) {
			final ModelPart subPart;
			if ( ELEMENT_TOKEN.equals( modelPartName ) ) {
				subPart = ( (EntityValuedModelPart) referenceModelPart ).getEntityMappingType().getIdentifierMapping();
			}
			else {
				subPart = ( (EntityValuedModelPart) referenceModelPart ).findSubPart( modelPartName );
			}
			return resolve( subPart, ast, tableGroup, modelPartName, creationState );
		}
		else if ( referenceModelPart instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart embeddableValuedModelPart = (EmbeddableValuedModelPart) referenceModelPart;
			if ( embeddableValuedModelPart.getFetchableName()
					.equals( modelPartName ) || ELEMENT_TOKEN.equals( modelPartName ) ) {
				final List<Expression> expressions = new ArrayList<>( embeddableValuedModelPart.getNumberOfFetchables() );
				embeddableValuedModelPart.visitFetchables(
						fetchable -> {
							expressions.add( resolve( fetchable, ast, tableGroup, modelPartName, creationState ) );
						},
						null
				);
				return new SqlTuple( expressions, embeddableValuedModelPart );
			}
			else {
				ModelPart subPart = embeddableValuedModelPart.findSubPart( modelPartName, null );
				assert subPart instanceof BasicValuedModelPart;
				return resolve( subPart, ast, tableGroup, modelPartName, creationState );
			}
		}
		else {
			// sure it can happen
			throw new NotYetImplementedFor6Exception( "Ordering for " + referenceModelPart + " not supported" );
		}
	}

	@Override
	public void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			NullPrecedence nullPrecedence,
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

	public void apply(
			ModelPart referenceModelPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			NullPrecedence nullPrecedence,
			SqlAstCreationState creationState) {
		if ( referenceModelPart instanceof BasicValuedModelPart ) {
			addSortSpecification(
					(BasicValuedModelPart) referenceModelPart,
					ast,
					tableGroup,
					collation,
					sortOrder,
					nullPrecedence,
					creationState
			);
		}
		else if ( referenceModelPart instanceof EntityValuedModelPart ) {
			final ModelPart subPart;
			if ( ELEMENT_TOKEN.equals( modelPartName ) ) {
				subPart = ( (EntityValuedModelPart) referenceModelPart ).getEntityMappingType().getIdentifierMapping();
			}
			else {
				subPart = ( (EntityValuedModelPart) referenceModelPart ).findSubPart( modelPartName );
			}
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
		else if ( referenceModelPart instanceof EmbeddableValuedModelPart ) {
			addSortSpecification(
					(EmbeddableValuedModelPart) referenceModelPart,
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
			throw new NotYetImplementedFor6Exception( "Ordering for " + getReferenceModelPart() + " not supported" );
		}
	}

	private void addSortSpecification(
			EmbeddableValuedModelPart embeddableValuedModelPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			NullPrecedence nullPrecedence,
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
			assert subPart instanceof BasicValuedModelPart;
			addSortSpecification(
					(BasicValuedModelPart) subPart,
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
			SortOrder sortOrder,
			NullPrecedence nullPrecedence,
			SqlAstCreationState creationState) {
		final TableReference tableReference = tableGroup.resolveTableReference( getNavigablePath(), selection.getContainingTableExpression() );
		final Expression expression = creationState.getSqlExpressionResolver().resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey(
						selection.getContainingTableExpression(),
						selection.getSelectionExpression()
				),
				sqlAstProcessingState -> new ColumnReference(
						tableReference,
						selection,
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
		ast.addSortSpecification( new SortSpecification( expression, collation, sortOrder, nullPrecedence ) );
	}
}
