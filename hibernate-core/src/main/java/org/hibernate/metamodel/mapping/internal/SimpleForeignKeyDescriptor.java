/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.internal.domain.basic.BasicResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SimpleForeignKeyDescriptor implements ForeignKeyDescriptor {
	private final String keyColumnContainingTable;
	private final String keyColumnExpression;
	private final String targetColumnContainingTable;
	private final String targetColumnExpression;
	private final JdbcMapping jdbcMapping;

	public SimpleForeignKeyDescriptor(
			String keyColumnContainingTable,
			String keyColumnExpression,
			String targetColumnContainingTable,
			String targetColumnExpression,
			JdbcMapping jdbcMapping) {
		this.keyColumnContainingTable = keyColumnContainingTable;
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
		final TableReference keyTableKeyReference = getKeyTableReference( tableGroup, tableGroup );

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								keyTableKeyReference,
								keyColumnExpression
						),
						s -> new ColumnReference(
								keyTableKeyReference,
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
		final TableReference targetTableReference = lhs.resolveTableReference( targetColumnContainingTable );

		final ColumnReference targetReference = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey( targetTableReference, keyColumnExpression ),
				s -> new ColumnReference(
						targetTableReference.getIdentificationVariable(),
						targetColumnExpression,
						jdbcMapping,
						creationContext.getSessionFactory()
				)
		);

		final TableReference keyTableKeyReference = getKeyTableReference( lhs, tableGroup );

		final ColumnReference keyReference = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey(
						keyTableKeyReference,
						keyColumnExpression
				),
				s -> new ColumnReference(
						keyTableKeyReference.getIdentificationVariable(),
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

	protected TableReference getKeyTableReference(TableGroup lhs, TableGroup tableGroup) {
		for ( TableReferenceJoin tableJoin : lhs.getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTableExpression().equals( keyColumnContainingTable ) ) {
				return tableJoin.getJoinedTableReference();
			}
		}
		return tableGroup.getPrimaryTableReference();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return jdbcMapping.getJavaTypeDescriptor();
	}

	@Override
	public void visitReferringColumns(ColumnConsumer consumer) {
		consumer.accept( keyColumnContainingTable, keyColumnExpression, jdbcMapping );
	}

	@Override
	public void visitTargetColumns(ColumnConsumer consumer) {
		consumer.accept( targetColumnContainingTable, targetColumnExpression, jdbcMapping );
	}

	@Override
	public void visitColumnMappings(FkColumnMappingConsumer consumer) {
		consumer.consume(
				keyColumnContainingTable,
				keyColumnExpression,
				targetColumnContainingTable,
				targetColumnExpression,
				jdbcMapping
		);
	}

	@Override
	public int getJdbcTypeCount(TypeConfiguration typeConfiguration) {
		return 1;
	}

	@Override
	public List<JdbcMapping> getJdbcMappings(TypeConfiguration typeConfiguration) {
		return Collections.singletonList( jdbcMapping );
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( jdbcMapping );
	}

	@Override
	public void visitJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( value, jdbcMapping );
	}
}
