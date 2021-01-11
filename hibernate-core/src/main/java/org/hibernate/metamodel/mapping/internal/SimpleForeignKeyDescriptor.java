/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.SelectionMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
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
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SimpleForeignKeyDescriptor implements ForeignKeyDescriptor, BasicValuedModelPart, FetchOptions {
	private final SelectionMapping keySelectionMapping;
	private final SelectionMapping targetSelectionMapping;
	private AssociationKey associationKey;

	public SimpleForeignKeyDescriptor(
			SelectionMapping keySelectionMapping,
			SelectionMapping targetSelectionMapping) {
		this.keySelectionMapping = keySelectionMapping;
		this.targetSelectionMapping = targetSelectionMapping;
	}

	@Override
	public DomainResult<?> createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetSelectionMapping.getContainingTableExpression()
				.equals( keySelectionMapping.getContainingTableExpression() ) ) {
			return createDomainResult( tableGroup, targetSelectionMapping, creationState );
		}
		return createDomainResult( collectionPath, tableGroup, creationState );
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		return createDomainResult( tableGroup, keySelectionMapping, creationState );
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			boolean isKeyReferringSide,
			DomainResultCreationState creationState) {
		if ( isKeyReferringSide ) {
			return createDomainResult( tableGroup, keySelectionMapping, creationState );
		}
		return createDomainResult( tableGroup, targetSelectionMapping, creationState );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return createDomainResult( tableGroup, keySelectionMapping, creationState );
	}

	private <T> DomainResult<T> createDomainResult(
			TableGroup tableGroup,
			SelectionMapping selectionMapping,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( selectionMapping.getContainingTableExpression() );
		final String identificationVariable = tableReference.getIdentificationVariable();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								selectionMapping.getSelectionExpression()
						),
						s ->
								new ColumnReference(
										identificationVariable,
										selectionMapping,
										creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
								)
				),
				selectionMapping.getJdbcMapping().getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				null,
				selectionMapping.getJdbcMapping().getJavaTypeDescriptor()
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		if ( lhs.getTableReference( keySelectionMapping.getContainingTableExpression() ) != null ) {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							keySelectionMapping,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							targetSelectionMapping,
							creationContext.getSessionFactory()
					)
			);
		}
		else {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							targetSelectionMapping,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							keySelectionMapping,
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
		if ( targetSelectionMapping.getContainingTableExpression().equals( keySelectionMapping.getContainingTableExpression() )  ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keySelectionMapping.getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSelectionMapping.getContainingTableExpression()
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keySelectionMapping.getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSelectionMapping.getContainingTableExpression()
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
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return targetSelectionMapping.getJdbcMapping().getJavaTypeDescriptor();
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
	public int visitReferringColumns(int offset, SelectionConsumer consumer) {
		consumer.accept( offset, keySelectionMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int visitTargetColumns(int offset, SelectionConsumer consumer) {
		consumer.accept( offset, targetSelectionMapping );
		return getJdbcTypeCount();
	}

	@Override
	public AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			final List<String> associationKeyColumns = Collections.singletonList( keySelectionMapping.getSelectionExpression() );
			associationKey = new AssociationKey( keySelectionMapping.getContainingTableExpression(), associationKeyColumns );
		}
		return associationKey;
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( targetSelectionMapping.getJdbcMapping() );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, targetSelectionMapping.getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, targetSelectionMapping.getJdbcMapping() );
		return getJdbcTypeCount();
	}


	@Override
	public String getContainingTableExpression() {
		return keySelectionMapping.getContainingTableExpression();
	}

	@Override
	public String getSelectionExpression() {
		return keySelectionMapping.getSelectionExpression();
	}

	@Override
	public boolean isFormula() {
		return keySelectionMapping.isFormula();
	}

	@Override
	public String getCustomReadExpression() {
		return keySelectionMapping.getCustomReadExpression();
	}

	@Override
	public String getCustomWriteExpression() {
		return keySelectionMapping.getCustomWriteExpression();
	}

	@Override
	public String getFetchableName() {
		return PART_NAME;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
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
	public MappingType getMappedType() {
		return null;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return keySelectionMapping.getJdbcMapping();
	}

	@Override
	public String toString() {
		return "SimpleForeignKeyDescriptor : " + keySelectionMapping.getContainingTableExpression() + "." + keySelectionMapping
				.getSelectionExpression()
				+ " --> " + targetSelectionMapping.getContainingTableExpression() + "." + targetSelectionMapping.getSelectionExpression();
	}
}
