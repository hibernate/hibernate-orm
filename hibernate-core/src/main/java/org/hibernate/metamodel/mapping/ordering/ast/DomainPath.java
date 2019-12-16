/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.SortOrder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Represents a domain-path (model part path) used in an order-by fragment
 *
 * @author Steve Ebersole
 */
public interface DomainPath extends OrderingExpression, SequencePart {
	NavigablePath getNavigablePath();

	DomainPath getLhs();

	ModelPart getReferenceModelPart();

	default PluralAttributeMapping getPluralAttribute() {
		return getLhs().getPluralAttribute();
	}

	@Override
	default void apply(
			QuerySpec ast,
			TableGroup tableGroup,
			String collation,
			SortOrder sortOrder,
			SqlAstCreationState creationState) {
		final SqlAstCreationContext creationContext = creationState.getCreationContext();
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final SqlExpressionResolver sqlExprResolver = creationState.getSqlExpressionResolver();

		if ( getReferenceModelPart() instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart basicValuedPart = (BasicValuedModelPart) getReferenceModelPart();

			final TableReference tableReference = tableGroup.resolveTableReference( basicValuedPart.getContainingTableExpression() );

			ast.addSortSpecification(
					new SortSpecification(
							new ColumnReference(
									tableReference,
									basicValuedPart.getMappedColumnExpression(),
									basicValuedPart.getJdbcMapping(),
									creationState.getCreationContext().getSessionFactory()
							),
							collation,
							sortOrder
					)
			);
		}
		else {
			getReferenceModelPart().visitColumns(
					(tableExpression, columnExpression, jdbcMapping) -> {
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
	}

}
