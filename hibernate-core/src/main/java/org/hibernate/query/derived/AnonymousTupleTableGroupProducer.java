/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
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
			List<SqlSelection> sqlSelections,
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
			final SqmSelectableNode<?> selectableNode = tupleType.getSelectableNode( i );
			final String partName = tupleType.getComponentName( i );
			final SqlSelection sqlSelection = sqlSelections.get( i );
			final ModelPart modelPart;
			if ( selectableNode instanceof SqmPath<?> ) {
				final SqmPath<?> sqmPath = (SqmPath<?>) selectableNode;
				final TableGroup tableGroup = fromClauseAccess.findTableGroup( sqmPath.getNavigablePath() );
				modelPart = createModelPart(
						selectableNode.getExpressible(),
						sqmPath.getNodeType().getSqmPathType(),
						sqlSelections,
						selectionIndex,
						partName,
						partName,
						tableGroup == null ? null : getModelPart( tableGroup ),
						compatibleTableExpressions
				);
			}
			else {
				modelPart = new AnonymousTupleBasicValuedModelPart(
						partName,
						partName,
						selectableNode.getExpressible(),
						sqlSelection.getExpressionType()
								.getJdbcMappings()
								.get( 0 )
				);
			}
			modelParts.put( partName, modelPart );
			selectionIndex += modelPart.getJdbcTypeCount();
		}
		this.modelParts = modelParts;
		this.compatibleTableExpressions = compatibleTableExpressions;
	}

	private ModelPart getModelPart(TableGroup tableGroup) {
		if ( tableGroup instanceof LazyTableGroup && ( (LazyTableGroup) tableGroup ).getUnderlyingTableGroup() != null ) {
			return ( (LazyTableGroup) tableGroup ).getUnderlyingTableGroup().getModelPart();
		}
		return tableGroup.getModelPart();
	}

	private ModelPart createModelPart(
			SqmExpressible<?> sqmExpressible,
			DomainType<?> domainType,
			List<SqlSelection> sqlSelections,
			int selectionIndex,
			String selectionExpression,
			String partName,
			ModelPart existingModelPart,
			Set<String> compatibleTableExpressions) {
		if ( domainType instanceof EntityDomainType<?> ) {
			final EntityValuedModelPart existingModelPartContainer = (EntityValuedModelPart) existingModelPart;
			final EntityIdentifierMapping identifierMapping = existingModelPartContainer.getEntityMappingType()
					.getIdentifierMapping();
			final EntityIdentifierMapping newIdentifierMapping;
			if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
				if ( identifierMapping.getPartMappingType() instanceof ManagedMappingType ) {
					// todo: implement
					throw new UnsupportedOperationException("Support for embedded id in anonymous tuples is not yet implemented");
				}
				else {
					newIdentifierMapping = new AnonymousTupleBasicEntityIdentifierMapping(
							selectionExpression + "_" + ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName(),
							sqmExpressible,
							sqlSelections.get( selectionIndex )
									.getExpressionType()
									.getJdbcMappings()
									.get( 0 ),
							(BasicEntityIdentifierMapping) identifierMapping
					);
				}
			}
			else {
				// todo: implement
				throw new UnsupportedOperationException("Support for id-class in anonymous tuples is not yet implemented");
			}
			if ( existingModelPartContainer instanceof ToOneAttributeMapping ) {
				// We take "ownership" of FK columns by reporting the derived table group is compatible
				compatibleTableExpressions.add( ( (ToOneAttributeMapping) existingModelPart ).getIdentifyingColumnsTableExpression() );
			}
			return new AnonymousTupleEntityValuedModelPart(
					newIdentifierMapping,
					domainType,
					selectionExpression,
					existingModelPartContainer
			);
		}
		else if ( domainType instanceof ManagedDomainType<?> ) {
			//noinspection unchecked
			final Set<Attribute<?, ?>> attributes = (Set<Attribute<?, ?>>) ( (ManagedDomainType<?>) domainType ).getAttributes();
			final Map<String, ModelPart> modelParts = CollectionHelper.linkedMapOfSize( attributes.size() );
			final EmbeddableValuedModelPart modelPartContainer = (EmbeddableValuedModelPart) existingModelPart;
			for ( Attribute<?, ?> attribute : attributes ) {
				if ( !( attribute instanceof SingularPersistentAttribute<?, ?> ) ) {
					throw new IllegalArgumentException( "Only embeddables without collections are supported!" );
				}
				final DomainType<?> attributeType = ( (SingularPersistentAttribute<?, ?>) attribute ).getType();
				final ModelPart modelPart = createModelPart(
						sqmExpressible,
						attributeType,
						sqlSelections,
						selectionIndex,
						selectionExpression + "_" + attribute.getName(),
						attribute.getName(),
						modelPartContainer.findSubPart( attribute.getName(), null ),
						compatibleTableExpressions
				);
				modelParts.put( modelPart.getPartName(), modelPart );
			}
			return new AnonymousTupleEmbeddableValuedModelPart( modelParts, domainType, selectionExpression, modelPartContainer );
		}
		else {
			return new AnonymousTupleBasicValuedModelPart(
					partName,
					selectionExpression,
					sqmExpressible,
					sqlSelections.get( selectionIndex )
							.getExpressionType()
							.getJdbcMappings()
							.get( 0 )
			);
		}
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
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		for ( ModelPart modelPart : modelParts.values() ) {
			consumer.accept( modelPart );
		}
	}

	@Override
	public String getSqlAliasStem() {
		return aliasStem;
	}

	@Override
	public JavaType<?> getJavaType() {
		return javaTypeDescriptor;
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
	public void breakDownJdbcValues(
			Object domainValue,
			JdbcValueConsumer valueConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

}
