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
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
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
	private final SelectableMapping keySelectableMapping;
	private final SelectableMapping targetSelectableMapping;
	private final PropertyAccess propertyAccess;
	private final boolean refersToPrimaryKey;
	private AssociationKey associationKey;

	public SimpleForeignKeyDescriptor(
			SelectableMapping keySelectableMapping,
			SelectableMapping targetSelectableMapping,
			PropertyAccess propertyAccess,
			boolean refersToPrimaryKey) {
		this.keySelectableMapping = keySelectableMapping;
		this.targetSelectableMapping = targetSelectableMapping;
		this.propertyAccess = propertyAccess;
		this.refersToPrimaryKey = refersToPrimaryKey;
	}

	@Override
	public String getKeyColumnContainingTable() {
		return keySelectableMapping.getContainingTableExpression();
	}

	@Override
	public String getTargetColumnContainingTable() {
		return targetSelectableMapping.getContainingTableExpression();
	}

	@Override
	public DomainResult<?> createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetSelectableMapping.getContainingTableExpression()
				.equals( keySelectableMapping.getContainingTableExpression() ) ) {
			return createDomainResult( tableGroup, targetSelectableMapping, creationState );
		}
		return createDomainResult( collectionPath, tableGroup, creationState );
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		return createDomainResult( tableGroup, keySelectableMapping, creationState );
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			boolean isKeyReferringSide,
			DomainResultCreationState creationState) {
		if ( isKeyReferringSide ) {
			return createDomainResult( tableGroup, keySelectableMapping, creationState );
		}
		return createDomainResult( tableGroup, targetSelectableMapping, creationState );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return createDomainResult( tableGroup, keySelectableMapping, creationState );
	}

	private <T> DomainResult<T> createDomainResult(
			TableGroup tableGroup,
			SelectableMapping selectableMapping,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( selectableMapping.getContainingTableExpression() );
		final String identificationVariable = tableReference.getIdentificationVariable();
		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableReference,
								selectableMapping.getSelectionExpression()
						),
						s ->
								new ColumnReference(
										identificationVariable,
										selectableMapping,
										creationState.getSqlAstCreationState().getCreationContext().getSessionFactory()
								)
				),
				selectableMapping.getJdbcMapping().getJavaTypeDescriptor(),
				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				null,
				selectableMapping.getJdbcMapping().getJavaTypeDescriptor()
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		if ( lhs.getTableReference( keySelectableMapping.getContainingTableExpression() ) != null ) {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							keySelectableMapping,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							targetSelectableMapping,
							creationContext.getSessionFactory()
					)
			);
		}
		else {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							targetSelectableMapping,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							keySelectableMapping,
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
		if ( targetSelectableMapping.getContainingTableExpression().equals( keySelectableMapping.getContainingTableExpression() )  ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keySelectableMapping.getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSelectableMapping.getContainingTableExpression()
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keySelectableMapping.getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSelectableMapping.getContainingTableExpression()
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
		return targetSelectableMapping.getJdbcMapping().getJavaTypeDescriptor();
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
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( refersToPrimaryKey && value instanceof HibernateProxy ) {
			return ( (HibernateProxy) value ).getHibernateLazyInitializer().getIdentifier();
		}
		return propertyAccess.getGetter().get( value );
	}

	@Override
	public Object getAssociationKeyFromTarget(Object targetObject, SharedSessionContractImplementor session) {
		return disassemble( targetObject, session );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, getJdbcMapping() );
		return 1;
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		valueConsumer.consume( domainValue, keySelectableMapping );
	}

	@Override
	public int visitReferringSelectables(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, keySelectableMapping );
		return getJdbcTypeCount();
	}

	@Override
	public int visitTargetSelectables(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, targetSelectableMapping );
		return getJdbcTypeCount();
	}

	@Override
	public AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			final List<String> associationKeyColumns = Collections.singletonList( keySelectableMapping.getSelectionExpression() );
			associationKey = new AssociationKey( keySelectableMapping.getContainingTableExpression(), associationKeyColumns );
		}
		return associationKey;
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( targetSelectableMapping.getJdbcMapping() );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, targetSelectableMapping.getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, targetSelectableMapping.getJdbcMapping() );
		return getJdbcTypeCount();
	}


	@Override
	public String getContainingTableExpression() {
		return keySelectableMapping.getContainingTableExpression();
	}

	@Override
	public String getSelectionExpression() {
		return keySelectableMapping.getSelectionExpression();
	}

	@Override
	public boolean isFormula() {
		return keySelectableMapping.isFormula();
	}

	@Override
	public String getCustomReadExpression() {
		return keySelectableMapping.getCustomReadExpression();
	}

	@Override
	public String getCustomWriteExpression() {
		return keySelectableMapping.getCustomWriteExpression();
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
		return keySelectableMapping.getJdbcMapping();
	}

	@Override
	public String toString() {
		return "SimpleForeignKeyDescriptor : " + keySelectableMapping.getContainingTableExpression() + "." + keySelectableMapping
				.getSelectionExpression()
				+ " --> " + targetSelectableMapping.getContainingTableExpression() + "." + targetSelectableMapping.getSelectionExpression();
	}
}
