/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PropertyBasedMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
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
	private final SimpleForeignKeyDescriptorSide keySide;
	private final SimpleForeignKeyDescriptorSide targetSide;

	private final boolean refersToPrimaryKey;

	private AssociationKey associationKey;

	public SimpleForeignKeyDescriptor(
			BasicValuedModelPart keyModelPart,
			PropertyAccess keyPropertyAccess,
			SelectableMapping keySelectableMapping,
			BasicValuedModelPart targetModelPart,
			boolean refersToPrimaryKey) {
		this( keyModelPart, keyPropertyAccess, keySelectableMapping, targetModelPart, refersToPrimaryKey, false );
	}

	public SimpleForeignKeyDescriptor(
			BasicValuedModelPart keyModelPart,
			PropertyAccess keyPropertyAccess,
			SelectableMapping keySelectableMapping,
			BasicValuedModelPart targetModelPart,
			boolean refersToPrimaryKey,
			boolean swapDirection) {
		assert keySelectableMapping != null;
		assert targetModelPart != null;

		keyModelPart = BasicAttributeMapping.withSelectableMapping(
				keyModelPart,
				keyPropertyAccess,
				keySelectableMapping
		);
		if ( swapDirection ) {
			this.keySide = new SimpleForeignKeyDescriptorSide( Nature.KEY, targetModelPart );
			this.targetSide = new SimpleForeignKeyDescriptorSide( Nature.TARGET, keyModelPart );
		}
		else {
			this.keySide = new SimpleForeignKeyDescriptorSide( Nature.KEY, keyModelPart );
			this.targetSide = new SimpleForeignKeyDescriptorSide( Nature.TARGET, targetModelPart );
		}
		this.refersToPrimaryKey = refersToPrimaryKey;
	}

	@Override
	public String getKeyTable() {
		return keySide.getModelPart().getContainingTableExpression();
	}

	@Override
	public String getTargetTable() {
		return targetSide.getModelPart().getContainingTableExpression();
	}

	@Override
	public BasicValuedModelPart getKeyPart() {
		return keySide.getModelPart();
	}

	@Override
	public BasicValuedModelPart getTargetPart() {
		return targetSide.getModelPart();
	}

	@Override
	public Side getKeySide() {
		return keySide;
	}

	@Override
	public Side getTargetSide() {
		return targetSide;
	}

	@Override
	public ForeignKeyDescriptor withKeySelectionMapping(
			IntFunction<SelectableMapping> selectableMappingAccess,
			MappingModelCreationProcess creationProcess) {
		return new SimpleForeignKeyDescriptor(
				keySide.getModelPart(),
				( (PropertyBasedMapping) keySide.getModelPart() ).getPropertyAccess(),
				selectableMappingAccess.apply( 0 ),
				targetSide.getModelPart(),
				refersToPrimaryKey
		);
	}

	@Override
	public DomainResult<?> createKeyDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		assert tableGroup.getTableReference( navigablePath, keySide.getModelPart().getContainingTableExpression() ) != null;

		return createDomainResult(
				navigablePath,
				tableGroup,
				keySide.getModelPart(),
				creationState
		);
	}

	@Override
	public DomainResult<?> createTargetDomainResult(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		assert tableGroup.getTableReference( navigablePath, targetSide.getModelPart().getContainingTableExpression() ) != null;

		return createDomainResult(
				navigablePath,
				tableGroup,
				targetSide.getModelPart(),
				creationState
		);
	}

	@Override
	public DomainResult<?> createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		return createDomainResult( collectionPath, tableGroup, targetSide.getModelPart(), creationState );
	}

	@Override
	public DomainResult<?> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			Nature side,
			DomainResultCreationState creationState) {
		if ( side == Nature.KEY ) {
			return createDomainResult( navigablePath, tableGroup, keySide.getModelPart(), creationState );
		}
		else {
			return createDomainResult( navigablePath, tableGroup, targetSide.getModelPart(), creationState );
		}
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return createDomainResult( navigablePath, tableGroup, keySide.getModelPart(), creationState );
	}

	private <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			SelectableMapping selectableMapping,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		final TableReference tableReference = tableGroup.resolveTableReference(
				navigablePath.append( getNavigableRole().getNavigableName() ),
				selectableMapping.getContainingTableExpression()
		);
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
		return new BasicResult<T>(
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
		if ( lhs.getTableReference( keySide.getModelPart().getContainingTableExpression() ) != null ) {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							keySide.getModelPart(),
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							targetSide.getModelPart(),
							creationContext.getSessionFactory()
					)
			);
		}
		else {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							targetSide.getModelPart(),
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							keySide.getModelPart(),
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
		if ( targetSide.getModelPart().getContainingTableExpression().equals( keySide.getModelPart().getContainingTableExpression() )  ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keySide.getModelPart().getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSide.getModelPart().getContainingTableExpression()
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keySide.getModelPart().getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSide.getModelPart().getContainingTableExpression()
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
		final NavigablePath navigablePath = lhs.getNavigablePath().append( getNavigableRole().getNavigableName() );
		if ( lhs.getPrimaryTableReference().getTableReference( navigablePath, table ) != null ) {
			return lhs.getPrimaryTableReference();
		}
		else if ( tableGroup.getPrimaryTableReference().getTableReference( navigablePath, table ) != null ) {
			return tableGroup.getPrimaryTableReference();
		}

		final TableReference tableReference = lhs.resolveTableReference( navigablePath, table );
		if ( tableReference != null ) {
			return tableReference;
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	@Override
	public MappingType getPartMappingType() {
		return targetSide.getModelPart().getMappedType();
	}

	@Override
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return targetSide.getModelPart().getJdbcMapping().getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return targetSide.getModelPart().getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return targetSide.getModelPart().findContainingEntityMapping();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( refersToPrimaryKey && value instanceof HibernateProxy ) {
			return ( (HibernateProxy) value ).getHibernateLazyInitializer().getIdentifier();
		}
		return ( (PropertyBasedMapping) targetSide.getModelPart() ).getPropertyAccess().getGetter().get( value );
	}

	@Override
	public Object getAssociationKeyFromSide(
			Object targetObject,
			Nature nature,
			SharedSessionContractImplementor session) {
		if ( targetObject == null ) {
			return null;
		}
		if ( refersToPrimaryKey && targetObject instanceof HibernateProxy ) {
			return ( (HibernateProxy) targetObject ).getHibernateLazyInitializer().getIdentifier();
		}
		final ModelPart modelPart;
		if ( nature == Nature.KEY ) {
			modelPart = keySide.getModelPart();
		}
		else {
			modelPart = targetSide.getModelPart();
		}
		return ( (PropertyBasedMapping) modelPart ).getPropertyAccess().getGetter().get( targetObject );
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
		valueConsumer.consume( domainValue, keySide.getModelPart() );
	}

	@Override
	public int visitKeySelectables(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, keySide.getModelPart() );
		return getJdbcTypeCount();
	}

	@Override
	public int visitTargetSelectables(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, targetSide.getModelPart() );
		return getJdbcTypeCount();
	}

	@Override
	public AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			final List<String> associationKeyColumns = Collections.singletonList( keySide.getModelPart().getSelectionExpression() );
			associationKey = new AssociationKey( keySide.getModelPart().getContainingTableExpression(), associationKeyColumns );
		}
		return associationKey;
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( targetSide.getModelPart().getJdbcMapping() );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, targetSide.getModelPart().getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, targetSide.getModelPart().getJdbcMapping() );
		return getJdbcTypeCount();
	}


	@Override
	public String getContainingTableExpression() {
		return keySide.getModelPart().getContainingTableExpression();
	}

	@Override
	public String getSelectionExpression() {
		return keySide.getModelPart().getSelectionExpression();
	}

	@Override
	public boolean isFormula() {
		return keySide.getModelPart().isFormula();
	}

	@Override
	public String getCustomReadExpression() {
		return keySide.getModelPart().getCustomReadExpression();
	}

	@Override
	public String getCustomWriteExpression() {
		return keySide.getModelPart().getCustomWriteExpression();
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
		return keySide.getModelPart().getJdbcMapping();
	}

	@Override
	public String toString() {
		return String.format(
				"SimpleForeignKeyDescriptor : %s.%s -> %s.%s",
				keySide.getModelPart().getContainingTableExpression(),
				keySide.getModelPart().getSelectionExpression(),
				targetSide.getModelPart().getContainingTableExpression(),
				targetSide.getModelPart().getSelectionExpression()
		);
	}
}
