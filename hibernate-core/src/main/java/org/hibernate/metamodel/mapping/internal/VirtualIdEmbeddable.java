/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.internal.MutableAttributeMappingList;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.spi.CompositeTypeImplementor;

import static org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping.IdentifierValueMapper;

/**
 * Embeddable describing the virtual-id aspect of a non-aggregated composite id
 */
public class VirtualIdEmbeddable extends AbstractEmbeddableMapping implements IdentifierValueMapper {
	private final NavigableRole navigableRole;
	private final NonAggregatedIdentifierMapping idMapping;
	private final VirtualIdRepresentationStrategy representationStrategy;

	private final MutableAttributeMappingList attributeMappings;
	private SelectableMappings selectableMappings;

	public VirtualIdEmbeddable(
			Component virtualIdSource,
			NonAggregatedIdentifierMapping idMapping,
			EntityPersister identifiedEntityMapping,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {
		super( creationProcess );

		this.navigableRole = idMapping.getNavigableRole();
		this.idMapping = idMapping;
		this.representationStrategy = new VirtualIdRepresentationStrategy(
				this,
				identifiedEntityMapping,
				virtualIdSource,
				creationProcess.getCreationContext()
		);

		final CompositeType compositeType = (CompositeType) virtualIdSource.getType();
		this.attributeMappings = new MutableAttributeMappingList( (compositeType).getPropertyNames().length );

		( (CompositeTypeImplementor) compositeType ).injectMappingModelPart( idMapping, creationProcess );

		creationProcess.registerInitializationCallback(
				"VirtualIdEmbeddable(" + navigableRole.getFullPath() + ")#finishInitialization",
				() ->
						finishInitialization(
								virtualIdSource,
								compositeType,
								rootTableExpression,
								rootTableKeyColumnNames,
								creationProcess
						)
		);
	}

	public VirtualIdEmbeddable(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			VirtualIdEmbeddable inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( creationProcess );

		this.navigableRole = inverseMappingType.getNavigableRole();
		this.idMapping = (NonAggregatedIdentifierMapping) valueMapping;
		this.representationStrategy = inverseMappingType.representationStrategy;
		this.attributeMappings = new MutableAttributeMappingList( inverseMappingType.attributeMappings.size() );
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"VirtualIdEmbeddable(" + inverseMappingType.getNavigableRole().getFullPath() + ".{inverse})#finishInitialization",
				() -> inverseInitializeCallback(
						declaringTableGroupProducer,
						selectableMappings,
						inverseMappingType,
						creationProcess,
						valueMapping.getDeclaringType(),
						this.attributeMappings
				)
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierValueMapper

	@Override
	public EmbeddableValuedModelPart getEmbeddedPart() {
		return idMapping;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return representationStrategy.getInstantiator().instantiate(
				() -> getValues( entity ),
				session.getSessionFactory()
		);
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		if ( entity != id ) {
			setValues( entity, getValues( id ) );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableMappingType

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getPartName() {
		return idMapping.getPartName();
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return getEmbeddedPart();
	}

	@Override
	public VirtualIdRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public AttributeMapping findAttributeMapping(String name) {
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attr = attributeMappings.get( i );
			if ( name.equals( attr.getAttributeName() ) ) {
				return attr;
			}
		}
		return null;
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return getSelectableMappings().getSelectable( columnIndex );
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return getSelectableMappings().forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return getSelectableMappings().forEachSelectable( offset, consumer );
	}

	@Override
	public int getJdbcTypeCount() {
		return getSelectableMappings().getJdbcTypeCount();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return getSelectableMappings().getJdbcMappings();
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return getSelectableMappings().getSelectable( index ).getJdbcMapping();
	}

	private SelectableMappings getSelectableMappings() {
		if (selectableMappings == null) {
			// This is expected to happen when processing a
			// PostInitCallbackEntry because the callbacks
			// are not ordered. The exception is caught in
			// MappingModelCreationProcess.executePostInitCallbacks()
			// and the callback is re-queued.
			throw new IllegalStateException("Not yet ready");
		}
		return selectableMappings;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return getSelectableMappings().forEachSelectable(
				offset,
				(index, selectable) -> action.accept( index, selectable.getJdbcMapping() )
		);
	}

	@Override
	public boolean isCreateEmptyCompositesEnabled() {
		// generally we do not want empty composites for identifiers
		return false;
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return attributeMappings.size();
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return attributeMappings.get( position );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public AttributeMappingsList getAttributeMappings() {
		return attributeMappings;
	}

	@Override
	public void forEachAttributeMapping(Consumer<? super AttributeMapping> action) {
		forEachAttribute( (index, attribute) -> action.accept( attribute ) );
	}

	@Override
	public void forEachAttributeMapping(final IndexedConsumer<? super AttributeMapping> consumer) {
		this.attributeMappings.indexedForEach( consumer );
	}

	@Override
	public int getNumberOfFetchables() {
		return getNumberOfAttributeMappings();
	}

	@Override
	public Fetchable getFetchable(int position) {
		return attributeMappings.get( position );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return idMapping.findContainingEntityMapping();
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		attributeMappings.forEach( consumer );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final SingularAttributeMapping attribute = attributeMappings.getSingularAttributeMapping( i );
			if ( attribute.getAttributeName().equals( name ) ) {
				return attribute;
			}
		}
		return null;
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			consumer.accept( i, attributeMappings.get( i ) );
		}
	}

	@Override
	public <T> DomainResult<T> createDomainResult(NavigablePath navigablePath, TableGroup tableGroup, String resultVariable, DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;

		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping instanceof PluralAttributeMapping ) {
				continue;
			}
			final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			span += attributeMapping.forEachJdbcValue( o, span + offset, valuesConsumer, session );
		}
		return span;
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		attributeMappings.forEach( (attribute) -> {
			final Object attributeValue = attribute.getValue( domainValue );
			attribute.breakDownJdbcValues( attributeValue, valueConsumer, session );
		} );
	}

	@Override
	public void decompose(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		if ( idMapping.getIdClassEmbeddable() != null ) {
			// during decompose, if there is an IdClass for the entity the
			// incoming `domainValue` should be an instance of that IdClass
			idMapping.getIdClassEmbeddable().decompose( domainValue, valueConsumer, session );
		}
		else {
			for ( int i = 0; i < attributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = attributeMappings.get( i );
				attributeMapping.decompose(
						attributeMapping.getValue( domainValue ),
						valueConsumer,
						session
				);
			}
		}
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final Object[] result = new Object[ attributeMappings.size() ];
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			final Object o = attributeMapping.getValue( value );
			result[i] = attributeMapping.disassemble( o, session );
		}

		return result;
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		int span = 0;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping mapping = attributeMappings.get( i );
			span += mapping.forEachDisassembledJdbcValue( values[i], span + offset, valuesConsumer, session );
		}
		return span;
	}

	@Override
	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		return new VirtualIdEmbeddable(
				valueMapping,
				declaringTableGroupProducer,
				selectableMappings,
				this,
				creationProcess
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// init

	private boolean finishInitialization(
			Component bootDescriptor,
			CompositeType compositeType,
			String rootTableExpression,
			String[] rootTableKeyColumnNames,
			MappingModelCreationProcess creationProcess) {

		// Reset the attribute mappings that were added in previous attempts
		this.attributeMappings.clear();

		return finishInitialization(
				navigableRole,
				bootDescriptor,
				compositeType,
				rootTableExpression,
				rootTableKeyColumnNames,
				this,
				representationStrategy,
				(attributeName, attributeType) -> {
					if ( attributeType instanceof CollectionType ) {
						throw new IllegalAttributeType( "A \"virtual id\" cannot define collection attributes : " + attributeName );
					}
					if ( attributeType instanceof AnyType ) {
						throw new IllegalAttributeType( "A \"virtual id\" cannot define <any/> attributes : " + attributeName );
					}
				},
				(column, jdbcEnvironment) -> MappingModelCreationHelper.getTableIdentifierExpression( column.getValue().getTable(), creationProcess ),
				this::addAttribute,
				() -> {
					// We need the attribute mapping types to finish initialization first before we can build the column mappings
					creationProcess.registerInitializationCallback(
							"VirtualIdEmbeddable(" + navigableRole + ")#initColumnMappings",
							this::initColumnMappings
					);
				},
				creationProcess
		);
	}

	private boolean initColumnMappings() {
		this.selectableMappings = SelectableMappingsImpl.from( this );
		return true;
	}

	private void addAttribute(AttributeMapping attributeMapping) {
		addAttribute( (SingularAttributeMapping) attributeMapping );
	}

	private void addAttribute(SingularAttributeMapping attributeMapping) {
		// check if we've already seen this attribute...
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping previous = attributeMappings.get( i );
			if ( attributeMapping.getAttributeName().equals( previous.getAttributeName() ) ) {
				attributeMappings.setAttributeMapping( i, attributeMapping );
				return;
			}
		}

		attributeMappings.add( attributeMapping );
	}
}
