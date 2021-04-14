/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
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
import org.hibernate.sql.results.DomainResultCreationException;
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
	private final BasicValuedModelPart keySide;
	private final BasicValuedModelPart targetSide;

	private final boolean refersToPrimaryKey;

	private final Function<Object,Object> disassemblyValueExtractor;

	private AssociationKey associationKey;

	public SimpleForeignKeyDescriptor(
			SelectableMapping keySelectableMapping,
			BasicValuedModelPart targetModelPart,
			Function<Object, Object> disassemblyValueExtractor,
			boolean refersToPrimaryKey) {
		assert keySelectableMapping != null;
		assert targetModelPart != null;
		assert disassemblyValueExtractor != null;

		this.keySide = BasicValuedSingularAttributeMapping.withSelectableMapping( targetModelPart, keySelectableMapping );
		this.targetSide = targetModelPart;
		this.disassemblyValueExtractor = disassemblyValueExtractor;
		this.refersToPrimaryKey = refersToPrimaryKey;
	}

	@Override
	public String getKeyTable() {
		return keySide.getContainingTableExpression();
	}

	@Override
	public String getTargetTable() {
		return targetSide.getContainingTableExpression();
	}

	public BasicValuedModelPart getKeySide() {
		return keySide;
	}

	public BasicValuedModelPart getTargetSide() {
		return targetSide;
	}

	@Override
	public ForeignKeyDescriptor withKeySelectionMapping(
			IntFunction<SelectableMapping> selectableMappingAccess,
			MappingModelCreationProcess creationProcess) {
		return new SimpleForeignKeyDescriptor(
				selectableMappingAccess.apply( 0 ),
				targetSide,
				disassemblyValueExtractor,
				refersToPrimaryKey
		);
	}

	@Override
	public DomainResult<?> createKeyDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		assert tableGroup.getTableReference( keySide.getContainingTableExpression() ) != null;

		return createDomainResult(
				collectionPath,
				tableGroup,
				keySide,
				creationState
		);
	}

	@Override
	public DomainResult<?> createTargetDomainResult(NavigablePath collectionPath, TableGroup tableGroup, DomainResultCreationState creationState) {
		assert tableGroup.getTableReference( targetSide.getContainingTableExpression() ) != null;

		return createDomainResult(
				collectionPath,
				tableGroup,
				targetSide,
				creationState
		);
	}

	@Override
	public DomainResult<?> createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetSide.getContainingTableExpression()
				.equals( keySide.getContainingTableExpression() ) ) {
			return createDomainResult( collectionPath, tableGroup, targetSide, creationState );
		}
		return createDomainResult( collectionPath, tableGroup, creationState );
	}

	@Override
	public DomainResult<?> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		try {
			return createDomainResult( navigablePath, tableGroup, keySide, creationState );
		}
		catch (Exception e) {
			throw new DomainResultCreationException(
					String.format(
							Locale.ROOT,
							"Unable to create fk key domain-result `%s.%s` relative to `%s`",
							keySide.getContainingTableExpression(),
							keySide.getSelectionExpression(),
							tableGroup
					),
					e
			);
		}
	}

	@Override
	public DomainResult<?> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			boolean isKeyReferringSide,
			DomainResultCreationState creationState) {
		if ( isKeyReferringSide ) {
			return createDomainResult( navigablePath, tableGroup, keySide, creationState );
		}
		return createDomainResult( navigablePath, tableGroup, targetSide, creationState );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return createDomainResult( navigablePath, tableGroup, keySide, creationState );
	}

	private <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
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
		if ( lhs.getTableReference( keySide.getContainingTableExpression() ) != null ) {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							keySide,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							targetSide,
							creationContext.getSessionFactory()
					)
			);
		}
		else {
			return new ComparisonPredicate(
					new ColumnReference(
							lhs,
							targetSide,
							creationContext.getSessionFactory()
					),
					ComparisonOperator.EQUAL,
					new ColumnReference(
							rhs,
							keySide,
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
		if ( targetSide.getContainingTableExpression().equals( keySide.getContainingTableExpression() )  ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keySide.getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSide.getContainingTableExpression()
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keySide.getContainingTableExpression() );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetSide.getContainingTableExpression()
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
	public ModelPart getKeyPart() {
		return keySide;
	}

	@Override
	public MappingType getPartMappingType() {
		return targetSide.getMappedType();
	}

	@Override
	public JavaTypeDescriptor<?> getJavaTypeDescriptor() {
		return targetSide.getJdbcMapping().getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return targetSide.getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return targetSide.findContainingEntityMapping();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( refersToPrimaryKey && value instanceof HibernateProxy ) {
			return ( (HibernateProxy) value ).getHibernateLazyInitializer().getIdentifier();
		}
		return disassemblyValueExtractor.apply( value );
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
		valueConsumer.consume( domainValue, keySide );
	}

	@Override
	public int visitKeySelectables(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, keySide );
		return getJdbcTypeCount();
	}

	@Override
	public int visitTargetSelectables(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, targetSide );
		return getJdbcTypeCount();
	}

	@Override
	public AssociationKey getAssociationKey() {
		if ( associationKey == null ) {
			final List<String> associationKeyColumns = Collections.singletonList( keySide.getSelectionExpression() );
			associationKey = new AssociationKey( keySide.getContainingTableExpression(), associationKeyColumns );
		}
		return associationKey;
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return Collections.singletonList( targetSide.getJdbcMapping() );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, targetSide.getJdbcMapping() );
		return getJdbcTypeCount();
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		valuesConsumer.consume( offset, value, targetSide.getJdbcMapping() );
		return getJdbcTypeCount();
	}


	@Override
	public String getContainingTableExpression() {
		return keySide.getContainingTableExpression();
	}

	@Override
	public String getSelectionExpression() {
		return keySide.getSelectionExpression();
	}

	@Override
	public boolean isFormula() {
		return keySide.isFormula();
	}

	@Override
	public String getCustomReadExpression() {
		return keySide.getCustomReadExpression();
	}

	@Override
	public String getCustomWriteExpression() {
		return keySide.getCustomWriteExpression();
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
		return keySide.getJdbcMapping();
	}

	@Override
	public String toString() {
		return String.format(
				"SimpleForeignKeyDescriptor : %s.%s -> %s.%s",
				keySide.getContainingTableExpression(),
				keySide.getSelectionExpression(),
				targetSide.getContainingTableExpression(),
				targetSide.getSelectionExpression()
		);
	}
}
