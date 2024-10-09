/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.derived;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.metamodel.Attribute;

/**
 * The table group producer for an anonymous tuple type.
 *
 * Model part names are determined based on the tuple type component names.
 * The kind and type of the model parts is based on the type of the underlying selection.
 *
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleTableGroupProducer implements TableGroupProducer, MappingType {

	private final String aliasStem;
	private final JavaType<?> javaTypeDescriptor;
	private final Map<String, ModelPart> modelParts;
	private final Set<String> compatibleTableExpressions;

	public AnonymousTupleTableGroupProducer(
			AnonymousTupleType<?> tupleType,
			String aliasStem,
			SqlTypedMapping[] sqlTypedMappings,
			FromClauseAccess fromClauseAccess) {
		this.aliasStem = aliasStem;
		this.javaTypeDescriptor = tupleType.getExpressibleJavaType();
		final Set<String> compatibleTableExpressions = new HashSet<>();
		// The empty table expression is the default for derived model parts
		compatibleTableExpressions.add( "" );

		final int componentCount = tupleType.componentCount();
		final Map<String, ModelPart> modelParts = CollectionHelper.linkedMapOfSize( componentCount );
		int selectionIndex = 0;
		for ( int i = 0; i < componentCount; i++ ) {
			final SqmExpressible<?> expressible = tupleType.get( i );
			final DomainType<?> sqmType = expressible.getSqmType();
//			final SqmSelectableNode<?> selectableNode = tupleType.getSelectableNode( i );
			final String partName = tupleType.getComponentName( i );
			final ModelPart modelPart;
			if ( expressible instanceof PluralPersistentAttribute<?, ?, ?>
					|| !( sqmType instanceof BasicType<?> ) ) {
				final TableGroup tableGroup = fromClauseAccess.findTableGroup( tupleType.getComponentSourcePath( i ) );
				modelPart = createModelPart(
						this,
						expressible,
						sqmType,
						sqlTypedMappings,
						selectionIndex,
						partName,
						partName,
						tableGroup == null ? null : getModelPart( tableGroup ),
						compatibleTableExpressions,
						modelParts.size()
				);
			}
			else if ( sqlTypedMappings[selectionIndex] instanceof SelectableMapping selectable ) {
				modelPart = new AnonymousTupleBasicValuedModelPart(
						this,
						partName,
						selectable,
						expressible,
						modelParts.size()
				);
				compatibleTableExpressions.add( selectable.getContainingTableExpression() );
			}
			else {
				modelPart = new AnonymousTupleBasicValuedModelPart(
						this,
						partName,
						partName,
						expressible,
						sqlTypedMappings[selectionIndex].getJdbcMapping(),
						modelParts.size()
				);
			}
			modelParts.put( partName, modelPart );
			selectionIndex += modelPart.getJdbcTypeCount();
		}
		this.modelParts = modelParts;
		this.compatibleTableExpressions = compatibleTableExpressions;
	}

	private ModelPart getModelPart(TableGroup tableGroup) {
		if ( tableGroup instanceof PluralTableGroup ) {
			tableGroup = ( (PluralTableGroup) tableGroup ).getElementTableGroup();
		}
		if ( tableGroup instanceof LazyTableGroup && ( (LazyTableGroup) tableGroup ).getUnderlyingTableGroup() != null ) {
			return ( (LazyTableGroup) tableGroup ).getUnderlyingTableGroup().getModelPart();
		}
		return tableGroup.getModelPart();
	}

	public static ModelPart createModelPart(
			MappingType mappingType,
			SqmExpressible<?> sqmExpressible,
			DomainType<?> domainType,
			SqlTypedMapping[] sqlTypedMappings,
			int selectionIndex,
			String selectionExpression,
			String partName,
			ModelPart existingModelPart,
			Set<String> compatibleTableExpressions,
			int fetchableIndex) {
		if ( domainType instanceof EntityDomainType<?> ) {
			final EntityValuedModelPart existingModelPartContainer = (EntityValuedModelPart) existingModelPart;
			final EntityIdentifierMapping identifierMapping = existingModelPartContainer.getEntityMappingType()
					.getIdentifierMapping();
			final EntityIdentifierMapping newIdentifierMapping;
			if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
				if ( identifierMapping.getPartMappingType() instanceof ManagedMappingType ) {
					//noinspection unchecked
					final Set<Attribute<?, ?>> attributes = (Set<Attribute<?, ?>>) ( (ManagedDomainType<?>) ( (EntityDomainType<?>) domainType ).getIdentifierDescriptor().getSqmPathType() ).getAttributes();
					newIdentifierMapping = new AnonymousTupleEmbeddedEntityIdentifierMapping(
							sqmExpressible,
							sqlTypedMappings,
							selectionIndex,
							selectionExpression + "_" + identifierMapping.getAttributeName(),
							compatibleTableExpressions,
							attributes,
							domainType,
							(CompositeIdentifierMapping) identifierMapping
					);
				}
				else if ( sqlTypedMappings[selectionIndex] instanceof SelectableMapping selectable ) {
					newIdentifierMapping = new AnonymousTupleBasicEntityIdentifierMapping(
							mappingType,
							selectable,
							sqmExpressible,
							(BasicEntityIdentifierMapping) identifierMapping
					);
					compatibleTableExpressions.add( selectable.getContainingTableExpression() );
				}
				else {
					newIdentifierMapping = new AnonymousTupleBasicEntityIdentifierMapping(
							mappingType,
							selectionExpression + "_" + identifierMapping.getAttributeName(),
							sqmExpressible,
							sqlTypedMappings[selectionIndex].getJdbcMapping(),
							(BasicEntityIdentifierMapping) identifierMapping
					);
				}
			}
			else {
				//noinspection unchecked
				final Set<Attribute<?, ?>> attributes = (Set<Attribute<?, ?>>) ( (ManagedDomainType<?>) ( (EntityDomainType<?>) domainType ).getIdentifierDescriptor().getSqmPathType() ).getAttributes();
				newIdentifierMapping = new AnonymousTupleNonAggregatedEntityIdentifierMapping(
						sqmExpressible,
						sqlTypedMappings,
						selectionIndex,
						selectionExpression,
						compatibleTableExpressions,
						attributes,
						domainType,
						selectionExpression,
						(NonAggregatedIdentifierMapping) identifierMapping
				);
			}
			if ( existingModelPartContainer instanceof ToOneAttributeMapping ) {
				// We take "ownership" of FK columns by reporting the derived table group is compatible
				compatibleTableExpressions.add( ( (ToOneAttributeMapping) existingModelPart ).getIdentifyingColumnsTableExpression() );
			}
			return new AnonymousTupleEntityValuedModelPart(
					newIdentifierMapping,
					domainType,
					existingModelPartContainer,
					fetchableIndex
			);
		}
		else if ( domainType instanceof ManagedDomainType<?> ) {
			//noinspection unchecked
			final Set<Attribute<?, ?>> attributes = (Set<Attribute<?, ?>>) ( (ManagedDomainType<?>) domainType ).getAttributes();
			return new AnonymousTupleEmbeddableValuedModelPart(
					sqmExpressible,
					sqlTypedMappings,
					selectionIndex,
					selectionExpression,
					compatibleTableExpressions,
					attributes,
					domainType,
					selectionExpression,
					(EmbeddableValuedModelPart) existingModelPart,
					fetchableIndex
			);
		}
		else if ( sqlTypedMappings[selectionIndex] instanceof SelectableMapping selectable ) {
			compatibleTableExpressions.add( selectable.getContainingTableExpression() );
			return new AnonymousTupleBasicValuedModelPart(
					mappingType,
					partName,
					selectable,
					sqmExpressible,
					fetchableIndex
			);
		}
		else {
			return new AnonymousTupleBasicValuedModelPart(
					mappingType,
					partName,
					selectionExpression,
					sqmExpressible,
					sqlTypedMappings[selectionIndex].getJdbcMapping(),
					fetchableIndex
			);
		}
	}

	public List<String> getColumnNames() {
		final ArrayList<String> columnNames = new ArrayList<>( modelParts.size() );
		forEachSelectable( (index, selectableMapping) -> columnNames.add( selectableMapping.getSelectionExpression() ) );
		return columnNames;
	}

	public Set<String> getCompatibleTableExpressions() {
		return compatibleTableExpressions;
	}

	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return javaTypeDescriptor;
	}

	@Override
	public String getPartName() {
		return null;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return null;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return modelParts.get( name );
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		int i = 0;
		for ( Map.Entry<String, ModelPart> entry : modelParts.entrySet() ) {
			consumer.accept( i++, entry.getValue() );
		}
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		for ( ModelPart modelPart : modelParts.values() ) {
			consumer.accept( modelPart );
		}
	}

	public Map<String, ModelPart> getModelParts() {
		return modelParts;
	}

	@Override
	public String getSqlAliasStem() {
		return aliasStem;
	}

	@Override
	public JavaType<?> getJavaType() {
		return javaTypeDescriptor;
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		final int originalOffset = offset;
		for ( ModelPart modelPart : modelParts.values() ) {
			offset += modelPart.forEachSelectable( offset, consumer );
		}

		return offset - originalOffset;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	//--------------------------------
	// Support for using the anonymous tuple as table reference directly somewhere is not yet implemented
	//--------------------------------

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}
}
