/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.internal.domain.basic.BasicResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class SimpleForeignKeyDescriptor implements ForeignKeyDescriptor {
	private final String keyColumnExpression;
	private final String targetColumnContainingTable;
	private final String targetColumnExpression;
	private final JdbcMapping jdbcMapping;

	public SimpleForeignKeyDescriptor(
			String keyColumnExpression,
			String targetColumnContainingTable,
			String targetColumnExpression,
			JdbcMapping jdbcMapping) {
		this.keyColumnExpression = keyColumnExpression;
		this.targetColumnContainingTable = targetColumnContainingTable;
		this.targetColumnExpression = targetColumnExpression;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableGroup.getPrimaryTableReference(),
								keyColumnExpression
						),
						s -> new ColumnReference(
								tableGroup.getPrimaryTableReference().getIdentificationVariable(),
								keyColumnExpression,
								jdbcMapping,
								creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
						)
				),
				jdbcMapping.getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				null,
				jdbcMapping.getJavaTypeDescriptor()
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableGroup lhs,
			TableGroup tableGroup,
			JoinType joinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final TableReference tableReference = lhs.resolveTableReference( targetColumnContainingTable );

		final ColumnReference targetReference = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( tableReference, keyColumnExpression ),
				s -> new ColumnReference(
						tableReference.getIdentificationVariable(),
						targetColumnExpression,
						jdbcMapping,
						creationContext.getSessionFactory()
				)
		);

		final ColumnReference keyReference = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey(
						tableGroup.getPrimaryTableReference(),
						keyColumnExpression
				),
				s -> new ColumnReference(
						tableGroup.getPrimaryTableReference().getIdentificationVariable(),
						keyColumnExpression,
						jdbcMapping,
						creationContext.getSessionFactory()
				)
		);

		return new ComparisonPredicate(
				targetReference,
				ComparisonOperator.EQUAL,
				keyReference
		);
	}
}
