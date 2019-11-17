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
import org.hibernate.type.ForeignKeyDirection;
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
	private final ForeignKeyDirection fKeyDirection;

	public SimpleForeignKeyDescriptor(
			ForeignKeyDirection fKeyDirection,
			String keyColumnContainingTable,
			String keyColumnExpression,
			String targetColumnContainingTable,
			String targetColumnExpression,
			JdbcMapping jdbcMapping) {
		this.fKeyDirection = fKeyDirection;
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
		final TableReference tableReference = tableGroup.resolveTableReference( keyColumnContainingTable );
		final String identificationVariable = tableReference.getIdentificationVariable();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								keyColumnExpression
						),
						s -> {
							return new ColumnReference(
									identificationVariable,
									keyColumnExpression,
									jdbcMapping,
									creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
							);
						}
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
		final TableReference keyTableReference = getTableReference( lhs, tableGroup, keyColumnContainingTable );
		ColumnReference keyColumnReference;
		keyColumnReference = new ColumnReference(
				keyTableReference,
				keyColumnExpression,
				jdbcMapping,
				creationContext.getSessionFactory()
		);

		ColumnReference targetColumnReference;
		if ( targetColumnContainingTable.equals( keyColumnContainingTable ) ) {
			final TableReference targetTableKeyReference = getTableReferenceWhenTargetEqualsKey(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
			targetColumnReference = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
					SqlExpressionResolver.createColumnReferenceKey(
							targetTableKeyReference,
							targetColumnExpression
					),
					s -> new ColumnReference(
							targetTableKeyReference.getIdentificationVariable(),
							targetColumnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);
		}
		else {
			final TableReference targetTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
			targetColumnReference = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
					SqlExpressionResolver.createColumnReferenceKey(
							targetTableKeyReference,
							targetColumnExpression
					),
					s -> new ColumnReference(
							targetTableKeyReference.getIdentificationVariable(),
							targetColumnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);
		}
		if ( fKeyDirection == ForeignKeyDirection.FROM_PARENT ) {
			return new ComparisonPredicate(
					targetColumnReference,
					ComparisonOperator.EQUAL,
					keyColumnReference
			);
		}
		return new ComparisonPredicate(
				keyColumnReference,
				ComparisonOperator.EQUAL,
				targetColumnReference
		);
	}

	protected TableReference getTableReferenceWhenTargetEqualsKey(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}

		for ( TableReferenceJoin tableJoin : lhs.getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTableExpression().equals( table ) ) {
				return tableJoin.getJoinedTableReference();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	protected TableReference getTableReference(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}
		else if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}

		for ( TableReferenceJoin tableJoin : lhs.getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTableExpression().equals( table ) ) {
				return tableJoin.getJoinedTableReference();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
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
