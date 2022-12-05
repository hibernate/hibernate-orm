/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMetadataAccess;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping.IdentifierValueMapper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.persister.internal.MutableAttributeMappingList;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.CompositeTypeImplementor;

import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getAttributeMetadataAccess;

/**
 * EmbeddableMappingType implementation describing an {@link jakarta.persistence.IdClass}
 */
public class IdClassEmbeddable extends AbstractEmbeddableMapping implements IdentifierValueMapper {
	private final NavigableRole navigableRole;
	private final NonAggregatedIdentifierMapping idMapping;
	private final VirtualIdEmbeddable virtualIdEmbeddable;
	private final JavaType<?> javaType;
	private final IdClassRepresentationStrategy representationStrategy;
//	private final IdClassEmbedded embedded;
	private final EmbeddableValuedModelPart embedded;

	private final MutableAttributeMappingList attributeMappings;
	private SelectableMappings selectableMappings;

	public IdClassEmbeddable(
			Component idClassSource,
			RootClass bootEntityDescriptor,
			NonAggregatedIdentifierMapping idMapping,
			EntityMappingType identifiedEntityMapping,
			String idTable,
			String[] idColumns,
			VirtualIdEmbeddable virtualIdEmbeddable,
			MappingModelCreationProcess creationProcess) {
		super( creationProcess );

		this.navigableRole = idMapping.getNavigableRole().append( NavigablePath.IDENTIFIER_MAPPER_PROPERTY );
		this.idMapping = idMapping;
		this.virtualIdEmbeddable = virtualIdEmbeddable;

		this.javaType = creationProcess.getCreationContext().getSessionFactory().getTypeConfiguration()
				.getJavaTypeRegistry()
				.resolveManagedTypeDescriptor( idClassSource.getComponentClass() );

		this.representationStrategy = new IdClassRepresentationStrategy( this );

		this.attributeMappings = new MutableAttributeMappingList( idClassSource.getPropertySpan() );

		final PropertyAccess propertyAccess = PropertyAccessStrategyMapImpl.INSTANCE.buildPropertyAccess(
				null,
				EntityIdentifierMapping.ROLE_LOCAL_NAME,
				true );
		final AttributeMetadataAccess attributeMetadataAccess = getAttributeMetadataAccess(
				propertyAccess
		);

		embedded = new EmbeddedAttributeMapping(
				NavigablePath.IDENTIFIER_MAPPER_PROPERTY,
				identifiedEntityMapping.getNavigableRole()
						.append( EntityIdentifierMapping.ROLE_LOCAL_NAME )
						.append( NavigablePath.IDENTIFIER_MAPPER_PROPERTY ),
				-1,
				idTable,
				attributeMetadataAccess,
				(PropertyAccess) null,
				FetchTiming.IMMEDIATE,
				FetchStyle.JOIN,
				this,
				identifiedEntityMapping,
				propertyAccess,
				null
		);

		final CompositeType idClassType = (CompositeType) idClassSource.getType();
		( (CompositeTypeImplementor) idClassType ).injectMappingModelPart( embedded, creationProcess );

		creationProcess.registerInitializationCallback(
				"IdClassEmbeddable(" + navigableRole.getFullPath() + ")#finishInitialization",
				() ->
						finishInitialization(
								idClassSource,
								idClassType,
								idTable,
								idColumns,
								creationProcess
						)
		);

	}

	public IdClassEmbeddable(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			IdClassEmbeddable inverseMappingType,
			MappingModelCreationProcess creationProcess) {
		super( creationProcess );


		this.navigableRole = inverseMappingType.getNavigableRole();
		this.idMapping = (NonAggregatedIdentifierMapping) valueMapping;;
		this.virtualIdEmbeddable = (VirtualIdEmbeddable) valueMapping.getEmbeddableTypeDescriptor();
		this.javaType = inverseMappingType.javaType;
		this.representationStrategy = new IdClassRepresentationStrategy( this );
		this.attributeMappings = new MutableAttributeMappingList( inverseMappingType.attributeMappings.size() );
		this.embedded = valueMapping;
		this.selectableMappings = selectableMappings;
		creationProcess.registerInitializationCallback(
				"IdClassEmbeddable(" + inverseMappingType.getNavigableRole().getFullPath() + ".{inverse})#finishInitialization",
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
		return embedded;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		final Object id = representationStrategy.getInstantiator().instantiate(
				null,
				session.getSessionFactory()
		);

		final Object[] propertyValues = new Object[virtualIdEmbeddable.getNumberOfAttributeMappings()];

		for ( int i = 0; i < propertyValues.length; i++ ) {
			final AttributeMapping attributeMapping = virtualIdEmbeddable.getAttributeMapping( i );
			final Object o = attributeMapping.getPropertyAccess().getGetter().get( entity );
			if ( o == null ) {
				final AttributeMapping idClassAttributeMapping = getAttributeMapping( i );
				if ( idClassAttributeMapping.getPropertyAccess().getGetter().getReturnTypeClass().isPrimitive() ) {
					propertyValues[i] = idClassAttributeMapping.getExpressibleJavaType().getDefaultValue();
				}
				else {
					propertyValues[i] = null;
				}
			}
			//JPA 2 @MapsId + @IdClass points to the pk of the entity
			else if ( attributeMapping instanceof ToOneAttributeMapping
					&& !( getAttributeMapping( i ) instanceof ToOneAttributeMapping ) ) {
				final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
				final ModelPart targetPart = toOneAttributeMapping.getForeignKeyDescriptor().getPart(
						toOneAttributeMapping.getSideNature().inverse()
				);
				if ( targetPart instanceof EntityIdentifierMapping ) {
					propertyValues[i] = ( (EntityIdentifierMapping) targetPart ).getIdentifier( o );
				}
				else {
					propertyValues[i] = o;
					assert false;
				}
			}
			else {
				propertyValues[i] = o;
			}
		}

		setValues( id, propertyValues );

		return id;
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		final EntityPersister entityDescriptor = factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entity.getClass() );
		final Object[] propertyValues = new Object[attributeMappings.size()];
		virtualIdEmbeddable.forEachAttribute(
				(position, virtualIdAttribute) -> {
					final AttributeMapping idClassAttribute = attributeMappings.getAttributeMapping( position );
					final Object o = idClassAttribute.getPropertyAccess().getGetter().get( id );
					if ( virtualIdAttribute instanceof ToOneAttributeMapping && !( idClassAttribute instanceof ToOneAttributeMapping ) ) {
						final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) virtualIdAttribute;
						final EntityPersister entityPersister = toOneAttributeMapping.getEntityMappingType()
								.getEntityPersister();
						final EntityKey entityKey = session.generateEntityKey( o, entityPersister );
						final PersistenceContext persistenceContext = session.getPersistenceContext();
						// it is conceivable there is a proxy, so check that first
						propertyValues[position] = persistenceContext.getProxy( entityKey );
						if ( propertyValues[position] == null ) {
							// otherwise look for an initialized version
							propertyValues[position] = persistenceContext.getEntity( entityKey );
							if ( propertyValues[position] == null ) {
								// get the association out of the entity itself
								propertyValues[position] = entityDescriptor.getPropertyValue(
										entity,
										toOneAttributeMapping.getAttributeName()
								);
							}
						}
					}
					else {
						propertyValues[position] = o;
					}
				}
		);

		virtualIdEmbeddable.setValues( entity, propertyValues );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableMappingType

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getPartName() {
		return NavigablePath.IDENTIFIER_MAPPER_PROPERTY;
	}

	@Override
	public EmbeddableRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return javaType;
	}

	@Override
	public EmbeddableValuedModelPart getEmbeddedValueMapping() {
		return embedded;
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return attributeMappings.size();
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return attributeMappings.getAttributeMapping( position );
	}

	@Override
	public boolean isCreateEmptyCompositesEnabled() {
		// generally we do not want empty composites for identifiers
		return false;
	}

	@Override
	public SingularAttributeMapping findAttributeMapping(String name) {
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final SingularAttributeMapping attribute = attributeMappings.getSingularAttributeMapping( i );
			if ( attribute.getAttributeName().equals( name ) ) {
				return attribute;
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public AttributeMappingsList getAttributeMappings() {
		return attributeMappings;
	}

	@Override
	public void visitAttributeMappings(Consumer<? super AttributeMapping> action) {
		forEachAttribute( (index, attribute) -> action.accept( attribute ) );
	}

	@Override
	public void forEachAttributeMapping(final IndexedConsumer<AttributeMapping> consumer) {
		this.attributeMappings.forEachAttributeMapping( consumer );
	}

	@Override
	public int getNumberOfFetchables() {
		return getNumberOfAttributeMappings();
	}

	@Override
	public Fetchable getFetchable(int position) {
		return attributeMappings.getAttributeMapping( position );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return idMapping.findContainingEntityMapping();
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		attributeMappings.forEachAttributeMapping( consumer );
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
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		attributeMappings.forEachAttributeMapping( (attribute) -> {
			final Object attributeValue = attribute.getValue( domainValue );
			attribute.breakDownJdbcValues( attributeValue, valueConsumer, session );
		} );
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return selectableMappings.getSelectable( columnIndex );
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return selectableMappings.forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return selectableMappings.forEachSelectable( offset, consumer );
	}

	@Override
	public int getJdbcTypeCount() {
		return selectableMappings.getJdbcTypeCount();
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return selectableMappings.getJdbcMappings();
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;

		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.getAttributeMapping( i );
			if ( attributeMapping instanceof PluralAttributeMapping ) {
				continue;
			}
			final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			span += attributeMapping.forEachJdbcValue( o, clause, span + offset, valuesConsumer, session );
		}
		return span;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		// todo (6.0) : reduce to-one values to id here?
		final Object[] result = new Object[ getNumberOfAttributeMappings() ];
		for ( int i = 0; i < result.length; i++ ) {
			final AttributeMapping attributeMapping = getAttributeMapping( i );
			Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			result[i] = attributeMapping.disassemble( o, session );
		}

		return result;
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return selectableMappings.forEachSelectable(
				offset,
				(index, selectable) -> action.accept( index, selectable.getJdbcMapping() )
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(NavigablePath navigablePath, TableGroup tableGroup, String resultVariable, DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public EmbeddableMappingType createInverseMappingType(
			EmbeddedAttributeMapping valueMapping,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			MappingModelCreationProcess creationProcess) {
		return new IdClassEmbeddable(
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
						throw new IllegalAttributeType( "An IdClass cannot define collection attributes : " + attributeName );
					}
					if ( attributeType instanceof AnyType ) {
						throw new IllegalAttributeType( "An IdClass cannot define <any/> attributes : " + attributeName );
					}
				},
				(column, jdbcEnvironment) -> MappingModelCreationHelper.getTableIdentifierExpression( column.getValue().getTable(), creationProcess ),
				this::addAttribute,
				() -> {
					// We need the attribute mapping types to finish initialization first before we can build the column mappings
					creationProcess.registerInitializationCallback(
							"IdClassEmbeddable(" + getNavigableRole() + ")#initColumnMappings",
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
			final AttributeMapping previous = attributeMappings.getAttributeMapping( i );
			if ( attributeMapping.getAttributeName().equals( previous.getAttributeName() ) ) {
				attributeMappings.setAttributeMapping( i, attributeMapping );
				return;
			}
		}

		attributeMappings.add( attributeMapping );
	}
}
