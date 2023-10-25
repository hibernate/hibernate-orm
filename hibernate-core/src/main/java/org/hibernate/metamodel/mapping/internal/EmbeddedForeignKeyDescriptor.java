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

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.OneToManyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
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
			String keyTable,
			SelectableMappings keySelectableMappings,
			EmbeddableValuedModelPart keyMappingType,
			String targetTable,
			SelectableMappings targetSelectableMappings,
			EmbeddableValuedModelPart targetMappingType,
			boolean hasConstraint,
			MappingModelCreationProcess creationProcess) {
		this(
				keyMappingType,
				targetMappingType,
				keyTable,
				keySelectableMappings,
				targetTable,
				targetSelectableMappings,
				hasConstraint,
				creationProcess
		);
	}

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
				(columnIndex, selection) -> columns.add( selection.getSelectionExpression() )
		);
		this.associationKey = new AssociationKey( keyTable, columns );
		this.hasConstraint = hasConstraint;

		creationProcess.registerInitializationCallback(
				"Embedded (composite) FK descriptor " + targetMappingType.getNavigableRole(),
				() -> {
					// todo (6.0) : how to make sure things we need are ready to go?
					// 		- e.g., here, we need access to the sub-attributes
					if ( targetMappingType.getEmbeddableTypeDescriptor().getNumberOfAttributeMappings() == 0 ) {
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

	private EmbeddedForeignKeyDescriptor(EmbeddedForeignKeyDescriptor original, EmbeddableValuedModelPart targetPart) {
		this.keyTable = original.keyTable;
		this.keySelectableMappings = original.keySelectableMappings;
		this.keySide = original.keySide;
		this.targetTable = targetPart.getContainingTableExpression();
		this.targetSelectableMappings = targetPart;
		this.targetSide = new EmbeddedForeignKeyDescriptorSide(
				Nature.TARGET,
				targetPart
		);
		this.associationKey = original.associationKey;
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
	public EmbeddableValuedModelPart getKeyPart() {
		return keySide.getModelPart().getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
	}

	@Override
	public EmbeddableValuedModelPart getTargetPart() {
		return targetSide.getModelPart().getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
	}

	@Override
	public boolean isKeyPart(ValuedModelPart modelPart) {
		final EmbeddableValuedModelPart keyPart = getKeyPart();
		if ( this == modelPart || keyPart == modelPart ) {
			return true;
		}
		else {
			AttributeMapping attributeMapping = modelPart.asAttributeMapping();
			while ( attributeMapping != null && attributeMapping.getDeclaringType() instanceof EmbeddableMappingType ) {
				final EmbeddableValuedModelPart declaringModelPart = ( (EmbeddableMappingType) attributeMapping.getDeclaringType() ).getEmbeddedValueMapping();
				if ( declaringModelPart == keyPart ) {
					return true;
				}
				attributeMapping = declaringModelPart.asAttributeMapping();
			}
		}
		return false;
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
	public int compare(Object key1, Object key2) {
		return getKeyPart().getEmbeddableTypeDescriptor().compare( key1, key2 );
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
	public ForeignKeyDescriptor withTargetPart(ValuedModelPart targetPart) {
		return new EmbeddedForeignKeyDescriptor( this, (EmbeddableValuedModelPart) targetPart );
	}

	@Override
	public DomainResult<?> createKeyDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		assert isTargetTableGroup( targetTableGroup );
		return createDomainResult(
				navigablePath,
				targetTableGroup,
				null,
				Nature.KEY,
				fetchParent,
				creationState
		);
	}

	@Override
	public DomainResult<?> createKeyDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			Nature fromSide,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		assert fromSide == Nature.TARGET
				? targetTableGroup.getTableReference( navigablePath, associationKey.getTable(), false ) != null
				: isTargetTableGroup( targetTableGroup );
		return createDomainResult(
				navigablePath.append( ForeignKeyDescriptor.PART_NAME ),
				targetTableGroup,
				null,
				Nature.KEY,
				fetchParent,
				creationState
		);
	}

	@Override
	public DomainResult<?> createTargetDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		assert isTargetTableGroup( targetTableGroup );
		return createDomainResult(
				navigablePath,
				targetTableGroup,
				null,
				Nature.TARGET,
				fetchParent,
				creationState
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup targetTableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		assert isTargetTableGroup( targetTableGroup );
		return createDomainResult(
				navigablePath,
				targetTableGroup,
				resultVariable,
				Nature.KEY,
				null,
				creationState
		);
	}

	private boolean isTargetTableGroup(TableGroup tableGroup) {
		tableGroup = getUnderlyingTableGroup( tableGroup );
		final TableGroupProducer tableGroupProducer;
		if ( tableGroup instanceof OneToManyTableGroup ) {
			tableGroupProducer = (TableGroupProducer) ( (OneToManyTableGroup) tableGroup ).getElementTableGroup()
					.getModelPart();
		}
		else {
			tableGroupProducer = (TableGroupProducer) tableGroup.getModelPart();
		}
		return tableGroupProducer.containsTableReference( targetSide.getModelPart().getContainingTableExpression() );
	}

	private static TableGroup getUnderlyingTableGroup(TableGroup tableGroup) {
		if ( tableGroup.isVirtual() ) {
			tableGroup = getUnderlyingTableGroup( ( (VirtualTableGroup) tableGroup ).getUnderlyingTableGroup() );
		}
		return tableGroup;
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
			Nature nature,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		final EmbeddableValuedModelPart modelPart;
		final NavigablePath resultNavigablePath;
		if ( nature == Nature.KEY ) {
			modelPart = keySide.getModelPart();
			resultNavigablePath = navigablePath.append( ForeignKeyDescriptor.PART_NAME );
		}
		else {
			modelPart = targetSide.getModelPart();
			resultNavigablePath = navigablePath.append( ForeignKeyDescriptor.TARGET_PART_NAME );
		}

		creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				resultNavigablePath,
				np -> {
					final TableGroupJoin tableGroupJoin = modelPart.createTableGroupJoin(
							resultNavigablePath,
							tableGroup,
							null,
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

		final Nature currentForeignKeyResolvingKey = creationState.getCurrentlyResolvingForeignKeyPart();
		try {
			creationState.setCurrentlyResolvingForeignKeyPart( nature );
			return new EmbeddableForeignKeyResultImpl<>(
					resultNavigablePath,
					modelPart,
					resultVariable,
					fetchParent,
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
			SqlAstCreationState creationState) {
		final TableReference lhsTableReference = targetSideTableGroup.resolveTableReference(
				targetSideTableGroup.getNavigablePath(),
				targetTable
		);
		final TableReference rhsTableKeyReference = keySideTableGroup.resolveTableReference(
				null,
				keyTable
		);

		return generateJoinPredicate( lhsTableReference, rhsTableKeyReference, creationState );
	}

	@Override
	public Predicate generateJoinPredicate(
			TableReference targetSideReference,
			TableReference keySideReference,
			SqlAstCreationState creationState) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		targetSelectableMappings.forEachSelectable(
				(i, selection) -> {
					final ComparisonPredicate comparisonPredicate = new ComparisonPredicate(
							new ColumnReference( targetSideReference, selection ),
							ComparisonOperator.EQUAL,
							new ColumnReference( keySideReference, keySelectableMappings.getSelectable( i ) )
					);
					predicate.add( comparisonPredicate );
				}
		);
		return predicate;
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

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return keySelectableMappings.getSelectable( columnIndex );
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
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		if ( domainValue == null ) {
			final int jdbcTypeCount = keySelectableMappings.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				valueConsumer.consume( offset + i, x, y, null, keySelectableMappings.getSelectable( i ) );
			}
			return jdbcTypeCount;
		}
		else if ( domainValue instanceof Object[] ) {
			final Object[] values = (Object[]) domainValue;
			final int jdbcTypeCount = keySelectableMappings.getJdbcTypeCount();
			for ( int i = 0; i < jdbcTypeCount; i++ ) {
				valueConsumer.consume( offset + i, x, y, values[i], keySelectableMappings.getSelectable( i ) );
			}
			return jdbcTypeCount;
		}
		else {
			final MutableInteger columnPosition = new MutableInteger();
			return keySide.getModelPart().breakDownJdbcValues(
					domainValue,
					offset,
					x,
					y,
					(valueIndex, arg1, arg2, jdbcValue, jdbcValueMapping) -> valueConsumer.consume(
							offset,
							arg1,
							arg2,
							jdbcValue,
							keySelectableMappings.getSelectable( columnPosition.getAndIncrement() )
					),
					session
			);
		}
	}

	@Override
	public Object getAssociationKeyFromSide(
			Object targetObject,
			ForeignKeyDescriptor.Side side,
			SharedSessionContractImplementor session) {
		final ModelPart modelPart = side.getModelPart();

		// If the mapping type has an identifier type, that identifier is the key
		if ( modelPart instanceof SingleAttributeIdentifierMapping ) {
			return ( (SingleAttributeIdentifierMapping) modelPart ).getIdentifierIfNotUnsaved( targetObject, session );
		}
		else if ( modelPart instanceof CompositeIdentifierMapping ) {
			return ( (CompositeIdentifierMapping) modelPart ).getIdentifierIfNotUnsaved( targetObject, session );
		}
		// Otherwise, this is a key based on the target object i.e. without id-class
		return targetObject;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return targetSide.getModelPart().findContainingEntityMapping();
	}

	@Override
	public JdbcMapping getJdbcMapping(final int index) {
		return targetSide.getModelPart().getJdbcMapping( index );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return targetSide.getModelPart().forEachJdbcType( offset, action );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return targetSide.getModelPart().forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return targetSide.getModelPart().disassemble( value, session );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		targetSide.getModelPart().addToCacheKey( cacheKey, value, session );
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return keySide.getModelPart().hasPartitionedSelectionMapping();
	}

	@Override
	public boolean isEmbedded() {
		return true;
	}
}
