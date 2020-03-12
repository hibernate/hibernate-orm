/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
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
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.BasicType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SimpleForeignKeyDescriptor implements ForeignKeyDescriptor, BasicValuedModelPart {
	private final String keyColumnContainingTable;
	private final String keyColumnExpression;
	private final String targetColumnContainingTable;
	private final String targetColumnExpression;
	private final JdbcMapping jdbcMapping;
	private final ForeignKeyDirection fKeyDirection;
	private final int hasCode;

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

		this.hasCode = Objects.hash(
				keyColumnContainingTable,
				keyColumnExpression,
				targetColumnContainingTable,
				targetColumnExpression
		);
	}

	@Override
	public ForeignKeyDirection getDirection() {
		return fKeyDirection;
	}

	@Override
	public DomainResult createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetColumnContainingTable.equals( keyColumnContainingTable ) ) {
			final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
			final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
			final TableReference tableReference = tableGroup.resolveTableReference( keyColumnContainingTable );
			final String identificationVariable = tableReference.getIdentificationVariable();
			final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
					sqlExpressionResolver.resolveSqlExpression(
							SqlExpressionResolver.createColumnReferenceKey(
									tableReference,
									targetColumnExpression
							),
							s -> {
								return new ColumnReference(
										identificationVariable,
										targetColumnExpression,
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
		else {
			return createDomainResult( collectionPath, tableGroup, creationState );
		}
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
						s ->
							new ColumnReference(
									identificationVariable,
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
			TableReference lhs,
			TableReference rhs,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String rhsTableExpression = rhs.getTableExpression();
		final String lhsTableExpression = lhs.getTableExpression();
		if ( lhsTableExpression.equals( keyColumnContainingTable ) ) {
			assert rhsTableExpression.equals( targetColumnContainingTable );
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							keyColumnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							targetColumnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);
		}
		else {
			assert rhsTableExpression.equals( keyColumnContainingTable );
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							targetColumnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							keyColumnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);
		}
	}

	@Override
	public Predicate generateJoinPredicate(
			TableGroup lhs,
			TableGroup tableGroup,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		TableReference lhsTableReference;
		TableReference rhsTableKeyReference;
		if ( targetColumnContainingTable.equals( keyColumnContainingTable )  ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}

		return generateJoinPredicate(
				lhsTableReference,
				rhsTableKeyReference,
				sqlAstJoinType,
				sqlExpressionResolver,
				creationContext
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

		final TableReference tableReference = lhs.resolveTableReference( table );
		if ( tableReference != null ) {
			return tableReference;
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return jdbcMapping.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getReferringTableExpression() {
		return keyColumnContainingTable;
	}

	@Override
	public void visitReferringColumns(ColumnConsumer consumer) {
		consumer.accept( keyColumnContainingTable, keyColumnExpression, jdbcMapping );
	}

	@Override
	public String getTargetTableExpression() {
		return targetColumnContainingTable;
	}

	@Override
	public void visitTargetColumns(ColumnConsumer consumer) {
		consumer.accept( targetColumnContainingTable, targetColumnExpression, jdbcMapping );
	}

	@Override
	public boolean areTargetColumnNamesEqualsTo(String[] columnNames) {
		if ( columnNames.length != 1 ) {
			return false;
		}
		return targetColumnExpression.equals( columnNames[0] );
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
	public BasicType getBasicType() {
		throw new HibernateException( "Unexpected call to SimpleForeignKeyDescriptor#getBasicType" );
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


	@Override
	public String getContainingTableExpression() {
		return keyColumnContainingTable;
	}

	@Override
	public String getMappedColumnExpression() {
		return keyColumnExpression;
	}

	@Override
	public BasicValueConverter getConverter() {
		return null;
	}

	@Override
	public String getFetchableName() {
		return PART_NAME;
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return FetchStrategy.IMMEDIATE_JOIN;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		return null;
	}

	@Override
	public MappingType getMappedTypeDescriptor() {
		return null;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	public String getTargetColumnContainingTable() {
		return targetColumnContainingTable;
	}

	public String getTargetColumnExpression() {
		return targetColumnExpression;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		SimpleForeignKeyDescriptor that = (SimpleForeignKeyDescriptor) o;
		return Objects.equals( keyColumnContainingTable, that.keyColumnContainingTable ) &&
				Objects.equals( targetColumnContainingTable, that.targetColumnContainingTable ) &&
				Objects.equals( keyColumnExpression, that.keyColumnExpression ) &&
				Objects.equals( targetColumnExpression, that.targetColumnExpression );
	}

	@Override
	public int hashCode() {
		return this.hasCode;
	}
}
