/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SortOrder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ordering.ast.DomainPath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
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
	public void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			SqlAstCreationState creationState) {
		final SqlAstCreationContext creationContext = creationState.getCreationContext();
		apply(
				getReferenceModelPart(),
				ast,
				tableGroup,
				collation,
				modelPartName,
				sortOrder,
				creationState,
				creationContext.getSessionFactory(),
				creationState.getSqlExpressionResolver()
		);
	}

	public void apply(
			ModelPart referenceModelPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			SqlAstCreationState creationState,
			SessionFactoryImplementor sessionFactory,
			SqlExpressionResolver sqlExprResolver) {
		if ( referenceModelPart instanceof BasicValuedModelPart ) {
			addSortSpecification(
					(BasicValuedModelPart) referenceModelPart,
					ast,
					tableGroup,
					collation,
					sortOrder,
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
					creationState,
					sessionFactory,
					sqlExprResolver
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
					creationState,
					sessionFactory,
					sqlExprResolver
			);
		}
		else {
			// sure it can happen
			throw new NotYetImplementedFor6Exception( "Ordering for " + getReferenceModelPart() + "not supported" );
		}
	}

	private void addSortSpecification(
			EmbeddableValuedModelPart embeddableValuedModelPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			String modelPartName,
			SortOrder sortOrder,
			SqlAstCreationState creationState,
			SessionFactoryImplementor sessionFactory,
			SqlExpressionResolver sqlExprResolver) {
		if ( embeddableValuedModelPart.getFetchableName()
				.equals( modelPartName ) || ELEMENT_TOKEN.equals( modelPartName ) ) {
			embeddableValuedModelPart.visitColumns(
					(tableExpression, columnExpression, isFormula, customReadExpression, customWriteExpression, jdbcMapping) -> {
						final TableReference tableReference = tableGroup.resolveTableReference( tableExpression );
						ast.addSortSpecification(
								new SortSpecification(
										sqlExprResolver.resolveSqlExpression(
												SqlExpressionResolver.createColumnReferenceKey(
														tableExpression,
														columnExpression
												),
												sqlAstProcessingState -> new ColumnReference(
														tableReference,
														columnExpression,
														isFormula,
														customReadExpression,
														customWriteExpression,
														jdbcMapping,
														sessionFactory
												)
										),
										collation,
										sortOrder
								)
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
					creationState
			);
		}
	}

	private void addSortSpecification(
			BasicValuedModelPart basicValuedPart,
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			SortOrder sortOrder,
			SqlAstCreationState creationState) {
		final TableReference tableReference = tableGroup.resolveTableReference( basicValuedPart.getContainingTableExpression() );

		ast.addSortSpecification(
				new SortSpecification(
						new ColumnReference(
								tableReference,
								basicValuedPart.getMappedColumnExpression(),
								basicValuedPart.isMappedColumnExpressionFormula(),
								basicValuedPart.getCustomReadExpression(),
								basicValuedPart.getCustomWriteExpression(),
								basicValuedPart.getJdbcMapping(),
								creationState.getCreationContext().getSessionFactory()
						),
						collation,
						sortOrder
				)
		);
	}
}
