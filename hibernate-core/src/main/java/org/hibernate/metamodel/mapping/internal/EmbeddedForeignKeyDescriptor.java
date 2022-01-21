/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableForeignKeyResultImpl;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Andrea Boriero
 */
public class EmbeddedForeignKeyDescriptor implements ForeignKeyDescriptor {

	private final EmbeddedForeignKeyDescriptorSide keySide;
	private final EmbeddedForeignKeyDescriptorSide targetSide;
	private final String keyTable;
	private final SelectableMappings keySelectableMappings;
	private final String targetTable;
	private final SelectableMappings targetSelectableMappings;
	private final AssociationKey associationKey;
	private final boolean hasConstraint;

	public EmbeddedForeignKeyDescriptor(
			EmbeddableValuedModelPart keyMappingType,
			EmbeddableValuedModelPart targetMappingType,
			String keyTable,
			SelectableMappings keySelectableMappings,
			String targetTable,
			SelectableMappings targetSelectableMappings,
			boolean hasConstraint,
			MappingModelCreationProcess creationProcess) {
		this.keyTable = keyTable;
		this.keySelectableMappings = keySelectableMappings;
		this.targetTable = targetTable;
		this.targetSelectableMappings = targetSelectableMappings;
		this.targetSide = new EmbeddedForeignKeyDescriptorSide( Nature.TARGET, targetMappingType );
		this.keySide = new EmbeddedForeignKeyDescriptorSide( Nature.KEY, keyMappingType );
		final List<String> columns = new ArrayList<>( keySelectableMappings.getJdbcTypeCount() );
		keySelectableMappings.forEachSelectable(
				(columnIndex, selection) -> {
					columns.add( selection.getSelectionExpression() );
				}
		);
		this.associationKey = new AssociationKey( keyTable, columns );
		this.hasConstraint = hasConstraint;

		creationProcess.registerInitializationCallback(
				"Embedded (composite) FK descriptor " + targetMappingType.getNavigableRole(),
				() -> {
					// todo (6.0) : how to make sure things we need are ready to go?
					// 		- e.g., here, we need access to the sub-attributes
					final List<AttributeMapping> subAttributes = targetMappingType.getEmbeddableTypeDescriptor().getAttributeMappings();
					if ( subAttributes.isEmpty() ) {
						// todo (6.0) : ^^ for now, this is the only way we "know" that the embeddable has not been finalized yet
						return false;
					}
					return true;
				}
		);
	}

	private EmbeddedForeignKeyDescriptor(
			EmbeddedForeignKeyDescriptor original,
			String keyTable,
			ManagedMappingType keyDeclaringType,
			TableGroupProducer keyDeclaringTableGroupProducer,
			SelectableMappings keySelectableMappings,
			MappingModelCreationProcess creationProcess) {
		this.keyTable = keyTable;
		this.keySelectableMappings = keySelectableMappings;
		this.targetTable = original.targetTable;
		this.targetSelectableMappings = original.targetSelectableMappings;
		this.targetSide = original.targetSide;
		this.keySide = new EmbeddedForeignKeyDescriptorSide(
				Nature.KEY,
				MappingModelCreationHelper.createInverseModelPart(
						original.targetSide.getModelPart(),
						keyDeclaringType,
						keyDeclaringTableGroupProducer,
						keySelectableMappings,
						creationProcess
				)
		);
		final List<String> columns = new ArrayList<>( keySelectableMappings.getJdbcTypeCount() );
		keySelectableMappings.forEachSelectable(
				(columnIndex, selection) -> {
					columns.add( selection.getSelectionExpression() );
				}
		);
		this.associationKey = new AssociationKey( keyTable, columns );
		this.hasConstraint = original.hasConstraint;
	}

	@Override
	public String getKeyTable() {
		return keyTable;
	}

	@Override
	public String getTargetTable() {
		return targetTable;
	}

	@Override
	public ModelPart getKeyPart() {
		return keySide.getModelPart().getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
	}

	@Override
	public ModelPart getTargetPart() {
		return targetSide.getModelPart().getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
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
			ManagedMappingType declaringType,
			TableGroupProducer declaringTableGroupProducer,
			IntFunction<SelectableMapping> selectableMappingAccess,
			MappingModelCreationProcess creationProcess) {
		SelectableMapping[] selectionMappings = new SelectableMapping[keySelectableMappings.getJdbcTypeCount()];
		for ( int i = 0; i < selectionMappings.length; i++ ) {
			selectionMappings[i] = selectableMappingAccess.apply( i );
		}
		return new EmbeddedForeignKeyDescriptor(
				this,
				selectionMappings[0].getContainingTableExpression(),
				declaringType,
				declaringTableGroupProducer,
				new SelectableMappingsImpl( selectionMappings ),
				creationProcess
		);
	}

	@Override
	public DomainResult<?> createKeyDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		return createDomainResult(
				navigablePath,
				tableGroup,
				null,
				keyTable,
				keySide.getModelPart(),
				creationState
		);
	}

	@Override
	public DomainResult<?> createTargetDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		assert tableGroup.getTableReference( navigablePath, targetTable ) != null;

		return createDomainResult(
				navigablePath,
				tableGroup,
				null,
				targetTable,
				targetSide.getModelPart(),
				creationState
		);
	}

	@Override
	public DomainResult<?> createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		if ( targetTable.equals( keyTable ) ) {
			return createDomainResult(
					collectionPath,
					tableGroup,
					null,
					targetTable,
					targetSide.getModelPart(),
					creationState
			);
		}
		else {
			return createDomainResult(
					collectionPath,
					tableGroup,
					null,
					keyTable,
					keySide.getModelPart(),
					creationState
			);
		}
	}

	@Override
	public DomainResult<?> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			Nature side,
			DomainResultCreationState creationState) {
		if ( side == Nature.KEY ) {
			return createDomainResult(
					navigablePath,
					tableGroup,
					null,
					keyTable,
					keySide.getModelPart(),
					creationState
			);
		}
		else {
			return createDomainResult(
					navigablePath,
					tableGroup,
					null,
					targetTable,
					targetSide.getModelPart(),
					creationState
			);
		}
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return createDomainResult( navigablePath, tableGroup, resultVariable, keyTable, keySide.getModelPart(), creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		throw new UnsupportedOperationException();
	}

	private <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			String columnContainingTable,
			EmbeddableValuedModelPart modelPart,
			DomainResultCreationState creationState) {
		final NavigablePath fkNavigablePath = navigablePath.append( getPartName() );
		final NavigablePath resultNavigablePath;
		if ( associationKey.getTable().equals( columnContainingTable ) ) {
			final ModelPartContainer parentModelPart = tableGroup.getModelPart();
			if ( parentModelPart instanceof PluralAttributeMapping ) {
				if ( ( (PluralAttributeMapping) parentModelPart ).getIndexDescriptor() == null ) {
					resultNavigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() )
							.append( getPartName() );
				}
				else {
					resultNavigablePath = navigablePath.append( CollectionPart.Nature.INDEX.getName() )
							.append( getPartName() );
				}
			}
			else {
				resultNavigablePath = navigablePath.append( getPartName() );
			}
		}
		else {
			resultNavigablePath = navigablePath.append( getPartName() );
		}
		final TableGroup fkTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				resultNavigablePath,
				np -> {
					final TableGroupJoin tableGroupJoin = modelPart.createTableGroupJoin(
							resultNavigablePath,
							tableGroup,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					tableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);
		if ( fkNavigablePath != resultNavigablePath ) {
			creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
					fkNavigablePath,
					np -> fkTableGroup
			);
		}

		final Nature currentForeignKeyResolvingKey = creationState.getCurrentlyResolvingForeignKeyPart();
		try {
			creationState.setCurrentlyResolvingForeignKeyPart( keySide.getModelPart() == modelPart ? Nature.KEY : Nature.TARGET );
			return new EmbeddableForeignKeyResultImpl<>(
					resultNavigablePath,
					modelPart,
					resultVariable,
					creationState
			);
		}
		finally {
			creationState.setCurrentlyResolvingForeignKeyPart( currentForeignKeyResolvingKey );
		}
	}

	@Override
	public Predicate generateJoinPredicate(
			TableGroup targetSideTableGroup,
			TableGroup keySideTableGroup,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final TableReference lhsTableReference = targetSideTableGroup.resolveTableReference(
				targetSideTableGroup.getNavigablePath(),
				targetTable
		);
		final TableReference rhsTableKeyReference = keySideTableGroup.resolveTableReference( keyTable );

		return generateJoinPredicate(
				lhsTableReference,
				rhsTableKeyReference,
				sqlAstJoinType,
				sqlExpressionResolver,
				creationContext
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableReference targetSideReference,
			TableReference keySideReference,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		return getPredicate( targetSideReference, keySideReference, creationContext, targetSelectableMappings, keySelectableMappings );
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		if ( !( predicate instanceof Junction ) ) {
			return false;
		}
		final Junction junction = (Junction) predicate;
		if ( junction.getNature() != Junction.Nature.CONJUNCTION ) {
			return false;
		}
		final List<Predicate> predicates = junction.getPredicates();
		if ( predicates.size() != keySelectableMappings.getJdbcTypeCount() ) {
			return false;
		}
		Boolean lhsIsKey = null;
		for ( int i = 0; i < predicates.size(); i++ ) {
			final Predicate p = predicates.get( i );
			if ( !( p instanceof ComparisonPredicate ) ) {
				return false;
			}
			final ComparisonPredicate comparisonPredicate = (ComparisonPredicate) p;
			if ( comparisonPredicate.getOperator() != ComparisonOperator.EQUAL ) {
				return false;
			}
			final Expression lhsExpr = comparisonPredicate.getLeftHandExpression();
			final Expression rhsExpr = comparisonPredicate.getRightHandExpression();
			if ( !( lhsExpr instanceof ColumnReference ) || !( rhsExpr instanceof ColumnReference ) ) {
				return false;
			}
			final ColumnReference lhs = (ColumnReference) lhsExpr;
			final ColumnReference rhs = (ColumnReference) rhsExpr;
			if ( lhsIsKey == null ) {
				final String keyExpression = keySelectableMappings.getSelectable( i ).getSelectionExpression();
				final String targetExpression = targetSelectableMappings.getSelectable( i ).getSelectionExpression();
				if ( keyExpression.equals( targetExpression ) ) {
					if ( !lhs.getColumnExpression().equals( keyExpression )
							|| !rhs.getColumnExpression().equals( keyExpression ) ) {
						return false;
					}
				}
				else {
					if ( keyExpression.equals( lhs.getColumnExpression() ) ) {
						if ( !targetExpression.equals( rhs.getColumnExpression() ) ) {
							return false;
						}
						lhsIsKey = true;
					}
					else if ( keyExpression.equals( rhs.getColumnExpression() ) ) {
						if ( !targetExpression.equals( lhs.getColumnExpression() ) ) {
							return false;
						}
						lhsIsKey = false;
					}
					else {
						return false;
					}
				}
			}
			else {
				final String lhsSelectionExpression;
				final String rhsSelectionExpression;
				if ( lhsIsKey ) {
					lhsSelectionExpression = keySelectableMappings.getSelectable( i ).getSelectionExpression();
					rhsSelectionExpression = targetSelectableMappings.getSelectable( i ).getSelectionExpression();
				}
				else {
					lhsSelectionExpression = targetSelectableMappings.getSelectable( i ).getSelectionExpression();
					rhsSelectionExpression = keySelectableMappings.getSelectable( i ).getSelectionExpression();
				}
				if ( !lhs.getColumnExpression().equals( lhsSelectionExpression )
						|| !rhs.getColumnExpression().equals( rhsSelectionExpression ) ) {
					return false;
				}
			}
		}

		return true;
	}

	private Predicate getPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstCreationContext creationContext,
			SelectableMappings lhsMappings,
			SelectableMappings rhsMappings) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		lhsMappings.forEachSelectable(
				(i, selection) -> {
					final ComparisonPredicate comparisonPredicate = new ComparisonPredicate(
							new ColumnReference(
									lhs,
									selection,
									creationContext.getSessionFactory()
							),
							ComparisonOperator.EQUAL,
							new ColumnReference(
									rhs,
									rhsMappings.getSelectable( i ),
									creationContext.getSessionFactory()
							)
					);
					predicate.add( comparisonPredicate );
				}
		);
		return predicate;
	}

	protected TableReference getTableReference(TableGroup lhs, TableGroup tableGroup, String table) {
		TableReference tableReference = lhs.getPrimaryTableReference().resolveTableReference( table );
		if ( tableReference != null ) {
			return tableReference;
		}
		tableReference = tableGroup.getPrimaryTableReference().resolveTableReference( table );
		if ( tableReference != null ) {
			return tableReference;
		}

		tableReference = lhs.resolveTableReference(
				lhs.getNavigablePath().append( getNavigableRole().getNavigableName() ),
				table
		);
		if ( tableReference != null ) {
			return tableReference;
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	@Override
	public int visitKeySelectables(int offset, SelectableConsumer consumer) {
		return keySelectableMappings.forEachSelectable( offset, consumer );
	}

	@Override
	public int visitTargetSelectables(int offset, SelectableConsumer consumer) {
		return targetSelectableMappings.forEachSelectable( offset, consumer );
	}

	@Override
	public boolean hasConstraint() {
		return hasConstraint;
	}

	@Override
	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	@Override
	public MappingType getMappedType() {
		return getPartMappingType();
	}

	@Override
	public MappingType getPartMappingType() {
		return targetSide.getModelPart().getPartMappingType();
	}

	@Override
	public JavaType<?> getJavaType() {
		return targetSide.getModelPart().getJavaType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return targetSide.getModelPart().getNavigableRole();
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		assert domainValue instanceof Object[];

		final Object[] values = (Object[]) domainValue;

		keySelectableMappings.forEachSelectable(
				(index, selectable) -> valueConsumer.consume( values[ index ], selectable )
		);
	}

	@Override
	public Object getAssociationKeyFromSide(
			Object targetObject,
			Nature nature,
			SharedSessionContractImplementor session) {
		final ModelPart modelPart;
		if ( nature == Nature.KEY ) {
			modelPart = keySide.getModelPart();
		}
		else {
			modelPart = targetSide.getModelPart();
		}
		// If the mapping type has an identifier type, that identifier is the key
		if ( modelPart instanceof SingleAttributeIdentifierMapping ) {
			return ( (SingleAttributeIdentifierMapping) modelPart ).getIdentifier( targetObject, session );
		}
		else if ( modelPart instanceof CompositeIdentifierMapping ) {
			return ( (CompositeIdentifierMapping) modelPart ).getIdentifier( targetObject, session );
		}
		// Otherwise, this is a key based on the target object i.e. without id-class
		return targetObject;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return targetSide.getModelPart().findContainingEntityMapping();
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return targetSide.getModelPart().forEachJdbcType( offset, action );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return targetSide.getModelPart().forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return targetSide.getModelPart().disassemble( value, session );
	}
}
