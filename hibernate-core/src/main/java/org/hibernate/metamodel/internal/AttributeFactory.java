/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.ValueClassification;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.model.domain.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AnyMappingDomainTypeImpl;
import org.hibernate.metamodel.model.domain.internal.EmbeddableTypeImpl;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.metamodel.model.domain.internal.MapMember;
import org.hibernate.metamodel.model.domain.internal.MappedSuperclassTypeImpl;
import org.hibernate.metamodel.model.domain.internal.PluralAttributeBuilder;
import org.hibernate.metamodel.model.domain.internal.SingularAttributeImpl;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessMapImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.EntityType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;
import org.hibernate.type.spi.CompositeTypeImplementor;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Type;

/**
 * A factory for building {@link Attribute} instances.  Exposes 3 main services for building<ol>
 * <li>{@link #buildAttribute normal attributes}</li>
 * <li>{@link #buildIdAttribute id attributes}</li>
 * <li>{@link #buildVersionAttribute version attributes}</li>
 * </ol>
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class AttributeFactory {
	private static final EntityManagerMessageLogger LOG = HEMLogging.messageLogger( AttributeFactory.class );

	private final MetadataContext context;

	public AttributeFactory(MetadataContext context) {
		this.context = context;
	}

	/**
	 * Build a normal attribute.
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param property The Hibernate property descriptor for the attribute
	 * @param <X> The type of the owner
	 * @param <Y> The attribute type
	 *
	 * @return The built attribute descriptor or null if the attribute is not part of the JPA 2 model (eg backrefs)
	 */
	public <X, Y> PersistentAttribute<X, Y> buildAttribute(ManagedDomainType<X> ownerType, Property property) {
		return buildAttribute( ownerType, property, context );
	}

	public static <X, Y> PersistentAttribute<X, Y> buildAttribute(
			ManagedDomainType<X> ownerType,
			Property property,
			MetadataContext metadataContext) {
		if ( property.isSynthetic() ) {
			// hide synthetic/virtual properties (fabricated by Hibernate) from the JPA metamodel.
			LOG.tracef( "Skipping synthetic property %s(%s)", ownerType.getTypeName(), property.getName() );
			return null;
		}
		LOG.tracef( "Building attribute [%s.%s]", ownerType.getTypeName(), property.getName() );
		final AttributeContext<X> attributeContext = wrap( ownerType, property );
		final AttributeMetadata<X, Y> attributeMetadata = determineAttributeMetadata(
				attributeContext,
				normalMemberResolver,
				metadataContext
		);

		if ( attributeMetadata instanceof PluralAttributeMetadata ) {
			return PluralAttributeBuilder.build(
					(PluralAttributeMetadata<X,Y,?>) attributeMetadata,
					property.isGeneric(),
					metadataContext
			);
		}

		final ValueContext valueContext = ( (SingularAttributeMetadata<X, Y>) attributeMetadata ).getValueContext();
		final DomainType<Y> metaModelType = determineSimpleType( valueContext, metadataContext );
		final JavaType<?> relationalJavaType = determineRelationalJavaType(
				valueContext,
				metaModelType,
				metadataContext
		);
		return new SingularAttributeImpl<>(
				ownerType,
				attributeMetadata.getName(),
				attributeMetadata.getAttributeClassification(),
				metaModelType,
				relationalJavaType,
				attributeMetadata.getMember(),
				false,
				false,
				property.isOptional(),
				property.isGeneric(),
				metadataContext
		);
	}

	private static <X> AttributeContext<X> wrap(final ManagedDomainType<X> ownerType, final Property property) {
		return new AttributeContext<>() {
			public ManagedDomainType<X> getOwnerType() {
				return ownerType;
			}

			public Property getPropertyMapping() {
				return property;
			}
		};
	}

	/**
	 * Build the identifier attribute descriptor
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param property The Hibernate property descriptor for the identifier attribute
	 * @param <X> The type of the owner
	 * @param <Y> The attribute type
	 *
	 * @return The built attribute descriptor
	 */
	public <X, Y> SingularPersistentAttribute<X, Y> buildIdAttribute(
			IdentifiableDomainType<X> ownerType,
			Property property) {
		LOG.tracef( "Building identifier attribute [%s.%s]", ownerType.getTypeName(), property.getName() );

		final AttributeMetadata<X, Y> attributeMetadata =
				determineAttributeMetadata( wrap( ownerType, property ), identifierMemberResolver );
		final SingularAttributeMetadata<X, Y> singularAttributeMetadata =
				(SingularAttributeMetadata<X, Y>) attributeMetadata;
		final DomainType<Y> domainType = determineSimpleType( singularAttributeMetadata.getValueContext() );
		return new SingularAttributeImpl.Identifier<>(
				ownerType,
				property.getName(),
				(SimpleDomainType<Y>) domainType,
				attributeMetadata.getMember(),
				attributeMetadata.getAttributeClassification(),
				property.isGeneric(),
				context
		);
	}

	/**
	 * Build the version attribute descriptor
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param property The Hibernate property descriptor for the version attribute
	 * @param <X> The type of the owner
	 * @param <Y> The attribute type
	 *
	 * @return The built attribute descriptor
	 */
	public <X, Y> SingularAttributeImpl<X, Y> buildVersionAttribute(
			IdentifiableDomainType<X> ownerType,
			Property property) {
		LOG.tracef( "Building version attribute [%s.%s]", ownerType.getTypeName(), property.getName() );

		final AttributeMetadata<X, Y> attributeMetadata =
				determineAttributeMetadata( wrap( ownerType, property ), versionMemberResolver );
		final SingularAttributeMetadata<X, Y> singularAttributeMetadata =
				(SingularAttributeMetadata<X, Y>) attributeMetadata;
		final DomainType<Y> domainType = determineSimpleType( singularAttributeMetadata.getValueContext() );
		return new SingularAttributeImpl.Version<>(
				ownerType,
				property.getName(),
				attributeMetadata.getAttributeClassification(),
				(SimpleDomainType<Y>) domainType,
				attributeMetadata.getMember(),
				context
		);
	}

	private <Y> DomainType<Y> determineSimpleType(ValueContext typeContext) {
		return determineSimpleType( typeContext, context );
	}

	public static <Y> DomainType<Y> determineSimpleType(ValueContext typeContext, MetadataContext context) {
		switch ( typeContext.getValueClassification() ) {
			case BASIC:
				return basicDomainType( typeContext, context );
			case ENTITY:
				return entityDomainType( typeContext, context );
			case EMBEDDABLE:
				return embeddableDomainType( typeContext, context );
			default:
				throw new AssertionFailure( "Unknown type : " + typeContext.getValueClassification() );
		}
	}

	private static <Y> EmbeddableDomainType<Y> embeddableDomainType(ValueContext typeContext, MetadataContext context) {
		final Component component = (Component) typeContext.getHibernateValue();
		if ( component.isDynamic() ) {
			return dynamicEmbeddableType( context, component );
		}
		else {
			// we should have a non-dynamic embeddable
			return classEmbeddableType( context, component );
		}
	}

	private static <Y> EmbeddableDomainType<Y> classEmbeddableType(MetadataContext context, Component component) {
		assert component.getComponentClassName() != null;
		@SuppressWarnings("unchecked")
		final Class<Y> embeddableClass = (Class<Y>) component.getComponentClass();

		if ( !component.isGeneric() ) {
			final EmbeddableDomainType<Y> cached = context.locateEmbeddable( embeddableClass, component );
			if ( cached != null ) {
				return cached;
			}
		}

		final DomainType<?> discriminatorType = component.isPolymorphic() ? component.getDiscriminatorType() : null;
		final EmbeddableTypeImpl<Y> embeddableType = new EmbeddableTypeImpl<>(
				context.getJavaTypeRegistry().resolveManagedTypeDescriptor( embeddableClass ),
				null,
				discriminatorType,
				false,
				context.getJpaMetamodel()
		);
		context.registerEmbeddableType( embeddableType, component );

		if ( component.isPolymorphic() ) {
			final java.util.Collection<String> embeddableSubclasses = component.getDiscriminatorValues().values();
			final java.util.Map<String, EmbeddableTypeImpl<?>> domainTypes = new HashMap<>();
			domainTypes.put( embeddableType.getTypeName(), embeddableType );
			final ClassLoaderService cls = context.getJpaMetamodel().getServiceRegistry().requireService(
					ClassLoaderService.class
			);
			for ( final String subclassName : embeddableSubclasses ) {
				if ( domainTypes.containsKey( subclassName ) ) {
					assert subclassName.equals( embeddableType.getTypeName() );
					continue;
				}
				final Class<?> subclass = cls.classForName( subclassName );
				final EmbeddableTypeImpl<?> subType = new EmbeddableTypeImpl<>(
						context.getJavaTypeRegistry().resolveManagedTypeDescriptor( subclass ),
						domainTypes.get( component.getSuperclass( subclassName ) ),
						discriminatorType,
						false,
						context.getJpaMetamodel()
				);
				domainTypes.put( subclassName, subType );
				context.registerEmbeddableType( subType, component );
			}
		}

		return embeddableType;
	}

	private static <Y> EmbeddableTypeImpl<Y> dynamicEmbeddableType(MetadataContext context, Component component) {
		final EmbeddableTypeImpl<Y> embeddableType = new EmbeddableTypeImpl<>(
				context.getJavaTypeRegistry().getDescriptor( java.util.Map.class ),
				null,
				null,
				true,
				context.getJpaMetamodel()
		);

		context.registerComponentByEmbeddable( embeddableType, component);

		final EmbeddableTypeImpl.InFlightAccess<Y> inFlightAccess = embeddableType.getInFlightAccess();
		for ( Property property : component.getProperties() ) {
			final PersistentAttribute<Y, Y> attribute = buildAttribute( embeddableType, property, context);
			if ( attribute != null ) {
				inFlightAccess.addAttribute( attribute );
			}
		}
		inFlightAccess.finishUp();

		return embeddableType;
	}

	private static <Y> DomainType<Y> entityDomainType(ValueContext typeContext, MetadataContext context) {
		final org.hibernate.type.Type type = typeContext.getHibernateValue().getType();
		if ( type instanceof EntityType ) {
			final EntityType entityType = (EntityType) type;
			final IdentifiableDomainType<Y> domainType =
					context.locateIdentifiableType( entityType.getAssociatedEntityName() );
			if ( domainType == null ) {
				// Due to the use of generics, it can happen that a mapped super class uses a type
				// for an attribute that is not a managed type. Since this case is not specifically mentioned
				// in the Jakarta Persistence spec, we handle this by returning a "dummy" entity type
				final JavaType<Y> domainJavaType =
						context.getJavaTypeRegistry().resolveDescriptor( typeContext.getJpaBindableType() );
				return new EntityTypeImpl<>( domainJavaType, context.getJpaMetamodel() );
			}
			else {
				return domainType;
			}
		}

		assert type instanceof AnyType;
		final AnyType anyType = (AnyType) type;
		return new AnyMappingDomainTypeImpl<>(
				(Any) typeContext.getHibernateValue(),
				anyType,
				context.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( anyType.getReturnedClass() ),
				context.getRuntimeModelCreationContext().getSessionFactory().getMappingMetamodel()
		);
	}

	@SuppressWarnings( "unchecked" )
	private static <Y> DomainType<Y> basicDomainType(ValueContext typeContext, MetadataContext context) {
		final Value hibernateValue = typeContext.getHibernateValue();
		if ( typeContext.getJpaBindableType().isPrimitive()
				&& ( (SimpleValue) hibernateValue ).getJpaAttributeConverterDescriptor() == null ) {
			// Special BasicDomainType necessary for primitive types in the JPA metamodel.
			// When a converted is applied to the attribute we already resolve to the correct type
			final Class<Y> type = (Class<Y>) typeContext.getJpaBindableType();
			return context.resolveBasicType( type );
		}
		else {
			final org.hibernate.type.Type type = hibernateValue.getType();
			if ( type instanceof BasicPluralType<?, ?> ) {
				final JavaType<?> javaTypeDescriptor = ( (BasicPluralType<?, ?>) type ).getElementType()
						.getJavaTypeDescriptor();
				if ( javaTypeDescriptor instanceof EmbeddableAggregateJavaType<?> ) {
					final AggregateColumn aggregateColumn = (AggregateColumn) hibernateValue.getColumns().get( 0 );
					classEmbeddableType( context, aggregateColumn.getComponent() );
				}
			}
			return (DomainType<Y>) type;
		}
	}

	private static JavaType<?> determineRelationalJavaType(
			ValueContext typeContext,
			DomainType<?> metaModelType,
			MetadataContext context) {
		if ( typeContext.getValueClassification() == ValueClassification.BASIC ) {
			final ConverterDescriptor descriptor =
					( (SimpleValue) typeContext.getHibernateValue() ).getJpaAttributeConverterDescriptor();
			if ( descriptor != null ) {
				return context.getJavaTypeRegistry().resolveDescriptor(
						descriptor.getRelationalValueResolvedType().getErasedType()
				);
			}
		}
		return metaModelType.getExpressibleJavaType();
	}

	private static EntityPersister getDeclaringEntity(
			AbstractIdentifiableType<?> ownerType,
			MetadataContext metadataContext) {
		return getDeclarerEntityPersister( ownerType, metadataContext );
	}

	private static EntityPersister getDeclarerEntityPersister(
			AbstractIdentifiableType<?> ownerType,
			MetadataContext metadataContext) {
		final Type.PersistenceType persistenceType = ownerType.getPersistenceType();
		if ( persistenceType == Type.PersistenceType.ENTITY ) {
			return metadataContext.getMetamodel().getEntityDescriptor( ownerType.getTypeName() );
		}
		else if ( persistenceType == Type.PersistenceType.MAPPED_SUPERCLASS ) {
			final PersistentClass persistentClass =
					metadataContext.getPersistentClassHostingProperties( (MappedSuperclassTypeImpl<?>) ownerType );
			return metadataContext.getMetamodel().findEntityDescriptor( persistentClass.getClassName() );
		}
		else {
			throw new AssertionFailure( "Cannot get the metamodel for PersistenceType: " + persistenceType );
		}
	}

	/**
	 * Here is most of the nuts and bolts of this factory, where we interpret the known JPA metadata
	 * against the known Hibernate metadata and build a descriptor for the attribute.
	 *
	 * @param attributeContext The attribute to be described
	 * @param memberResolver Strategy for how to resolve the member defining the attribute.
	 * @param <X> The owner type
	 * @param <Y> The attribute type
	 *
	 * @return The attribute description
	 */
	private <X, Y> AttributeMetadata<X, Y> determineAttributeMetadata(
			AttributeContext<X> attributeContext,
			MemberResolver memberResolver) {
		return determineAttributeMetadata( attributeContext, memberResolver, context );
	}

	private static <X, Y> AttributeMetadata<X, Y> determineAttributeMetadata(
			AttributeContext<X> attributeContext,
			MemberResolver memberResolver,
			MetadataContext context) {
		final Property propertyMapping = attributeContext.getPropertyMapping();
		final String propertyName = propertyMapping.getName();

		LOG.tracef( "Starting attribute metadata determination [%s]", propertyName );

		final Member member = memberResolver.resolveMember( attributeContext, context );
		LOG.tracef( "    Determined member [%s]", member );

		final Value value = propertyMapping.getValue();
		final org.hibernate.type.Type type = value.getType();
		LOG.tracef( "    Determined type [name=%s, class=%s]", type.getName(), type.getClass().getName() );

		if ( type.isAnyType() ) {
			return new SingularAttributeMetadataImpl<>(
					propertyMapping,
					attributeContext.getOwnerType(),
					member,
					AttributeClassification.ANY,
					context
			);
		}
		else if ( type.isAssociationType() ) {
			// collection or entity
			if ( type.isEntityType() ) {
				// entity
				return new SingularAttributeMetadataImpl<>(
						propertyMapping,
						attributeContext.getOwnerType(),
						member,
						determineSingularAssociationClassification( member ),
						context
				);
			}
			// collection
			if ( value instanceof Collection ) {
				final Collection collValue = (Collection) value;
				final Value elementValue = collValue.getElement();
				final org.hibernate.type.Type elementType = elementValue.getType();
				final boolean isManyToMany = isManyToMany( member );

				return new PluralAttributeMetadataImpl<>(
						propertyMapping,
						attributeContext.getOwnerType(),
						member,
						collectionClassification( elementType, elementValue, isManyToMany ),
						elementClassification( elementType, elementValue, isManyToMany ),
						indexClassification( value ),
						context
				);
			}
			else if ( value instanceof OneToMany ) {
				// TODO : is this even possible? Really OneToMany should be describing the
				//        element value within a o.h.mapping.Collection (see logic branch above)
				throw new AssertionFailure( "Unexpected OneToMany" );
//					final boolean isManyToMany = isManyToMany( member );
//					//one to many with FK => entity
//					return new PluralAttributeMetadataImpl(
//							attributeContext.getPropertyMapping(),
//							attributeContext.getOwnerType(),
//							member,
//							isManyToMany
//									? Attribute.PersistentAttributeType.MANY_TO_MANY
//									: Attribute.PersistentAttributeType.ONE_TO_MANY
//							value,
//							AttributeContext.TypeStatus.ENTITY,
//							Attribute.PersistentAttributeType.ONE_TO_MANY,
//							null, null, null
//					);
			}
		}
		else if ( propertyMapping.isComposite() ) {
			// component
			return new SingularAttributeMetadataImpl<>(
					propertyMapping,
					attributeContext.getOwnerType(),
					member,
					AttributeClassification.EMBEDDED,
					context
			);
		}
		else {
			// basic type
			return new SingularAttributeMetadataImpl<>(
					propertyMapping,
					attributeContext.getOwnerType(),
					member,
					AttributeClassification.BASIC,
					context
			);
		}
		throw new UnsupportedMappingException( "oops, we are missing something: " + propertyMapping );
	}

	private static AttributeClassification indexClassification(Value value) {
		if ( value instanceof Map ) {
			final Value keyValue = ( (Map) value).getIndex();
			return keyClassification( keyValue.getType(), keyValue );
		}
		else if ( value instanceof List ) {
			return AttributeClassification.BASIC;
		}
		else {
			return null;
		}
	}

	private static AttributeClassification elementClassification(
			org.hibernate.type.Type elementType, Value elementValue, boolean isManyToMany) {
		final AttributeClassification elementClassification;
		if ( elementType.isAnyType() ) {
			return AttributeClassification.ANY;
		}
		else if ( elementValue instanceof Component ) {
			return AttributeClassification.EMBEDDED;
		}
		else if ( elementType.isAssociationType() ) {
			return isManyToMany ?
					AttributeClassification.MANY_TO_MANY :
					AttributeClassification.ONE_TO_MANY;
		}
		else {
			return AttributeClassification.BASIC;
		}
	}

	private static AttributeClassification collectionClassification(
			org.hibernate.type.Type elementType, Value elementValue, boolean isManyToMany) {
		if ( elementType.isAnyType() ) {
			return AttributeClassification.ELEMENT_COLLECTION;
		}
		else if ( elementValue instanceof Component ) {
			return AttributeClassification.ELEMENT_COLLECTION;
		}
		else if ( elementType.isAssociationType() ) {
			return isManyToMany ?
					AttributeClassification.MANY_TO_MANY :
					AttributeClassification.ONE_TO_MANY;
		}
		else {
			return AttributeClassification.ELEMENT_COLLECTION;
		}
	}

	private static AttributeClassification keyClassification(org.hibernate.type.Type keyType, Value keyValue) {
		if ( keyType.isAnyType() ) {
			return AttributeClassification.ANY;
		}
		else if ( keyValue instanceof Component ) {
			return AttributeClassification.EMBEDDED;
		}
		else if ( keyType.isAssociationType() ) {
			return AttributeClassification.MANY_TO_ONE;
		}
		else {
			return AttributeClassification.BASIC;
		}
	}

	public static AttributeClassification determineSingularAssociationClassification(Member member) {
		if ( member instanceof Field ) {
			return ( (Field) member ).getAnnotation( OneToOne.class ) != null
					? AttributeClassification.ONE_TO_ONE
					: AttributeClassification.MANY_TO_ONE;
		}
		else if ( member instanceof MapMember ) {
			return AttributeClassification.MANY_TO_ONE; // curious to see how this works for non-annotated methods
		}
		else {
			return ( (Method) member ).getAnnotation( OneToOne.class ) != null
					? AttributeClassification.ONE_TO_ONE
					: AttributeClassification.MANY_TO_ONE;
		}
	}

	public static ParameterizedType getSignatureType(Member member) {
		final java.lang.reflect.Type type;
		if ( member instanceof Field ) {
			type = ( (Field) member ).getGenericType();
		}
		else if ( member instanceof Method ) {
			type = ( (Method) member ).getGenericReturnType();
		}
		else {
			type = ( (MapMember) member ).getType();
		}
		//this is a raw type
		if ( type instanceof Class ) {
			return null;
		}
		else {
			return (ParameterizedType) type;
		}
	}

	public static boolean isManyToMany(Member member) {
		if ( member instanceof Field ) {
			return ( (Field) member ).getAnnotation( ManyToMany.class ) != null;
		}
		else if ( member instanceof Method ) {
			return ( (Method) member ).getAnnotation( ManyToMany.class ) != null;
		}
		else {
			return false;
		}
	}

	private static final MemberResolver embeddedMemberResolver = (attributeContext, metadataContext) -> {
		// the owner is an embeddable
		final EmbeddableDomainType<?> ownerType = (EmbeddableDomainType<?>) attributeContext.getOwnerType();
		final Component ownerBootDescriptor = metadataContext.getEmbeddableBootDescriptor( ownerType );

		final CompositeTypeImplementor ownerComponentType = (CompositeTypeImplementor) ownerBootDescriptor.getType();
		final EmbeddableValuedModelPart ownerMappingModelDescriptor = ownerComponentType.getMappingModelPart();

		final EmbeddableRepresentationStrategy ownerRepStrategy =
				ownerRepresentationStrategy( metadataContext, ownerMappingModelDescriptor, ownerBootDescriptor );

		if ( ownerRepStrategy.getMode() == RepresentationMode.MAP ) {
			final Property propertyMapping = attributeContext.getPropertyMapping();
			return new MapMember( propertyMapping.getName(), propertyMapping.getType().getReturnedClass() );
		}
		else {
			return ownerRepStrategy
					.resolvePropertyAccess( attributeContext.getPropertyMapping() )
					.getGetter()
					.getMember();
		}
	};

	private static EmbeddableRepresentationStrategy ownerRepresentationStrategy(
			MetadataContext metadataContext, EmbeddableValuedModelPart ownerMappingModelDescriptor, Component ownerBootDescriptor) {
		if ( ownerMappingModelDescriptor == null ) {
			// When an entity uses a type variable, bound by a mapped superclass, for an embedded id,
			// we will not create a model part for the component, but we still need the representation strategy here,
			// in order to discover the property members to expose on the JPA metamodel
			return ownerBootDescriptor.getBuildingContext()
							.getBootstrapContext()
							.getRepresentationStrategySelector()
							.resolveStrategy(ownerBootDescriptor, null,
									metadataContext.getRuntimeModelCreationContext() );
		}
		else {
			return ownerMappingModelDescriptor
					.getEmbeddableTypeDescriptor()
					.getRepresentationStrategy();
		}
	}


	private static final MemberResolver virtualIdentifierMemberResolver = (attributeContext, metadataContext) -> {
		final AbstractIdentifiableType<?> identifiableType = (AbstractIdentifiableType<?>) attributeContext.getOwnerType();
		final EntityPersister entityPersister = getDeclarerEntityPersister( identifiableType, metadataContext );
		final EntityIdentifierMapping identifierMapping = entityPersister.getIdentifierMapping();

		if ( identifierMapping.getNature() != EntityIdentifierMapping.Nature.VIRTUAL ) {
			throw new IllegalArgumentException( "expecting IdClass mapping" );
		}

		final CompositeIdentifierMapping cid = (CompositeIdentifierMapping) identifierMapping;
		final EmbeddableMappingType embeddable = cid.getPartMappingType();
		final String attributeName = attributeContext.getPropertyMapping().getName();
		final AttributeMapping attributeMapping = embeddable.findAttributeMapping( attributeName );
		if ( attributeMapping == null ) {
			throw new PropertyNotFoundException(
					"Could not resolve attribute '" + attributeName
							+ "' of '" + embeddable.getJavaType().getJavaTypeClass().getName() + "'"
			);
		}

		final Getter getter = attributeMapping.getPropertyAccess().getGetter();
		return getter instanceof PropertyAccessMapImpl.GetterImpl
				? new MapMember( attributeName, attributeContext.getPropertyMapping().getType().getReturnedClass() )
				: getter.getMember();
	};

	/**
	 * A {@link Member} resolver for normal attributes.
	 */
	private static final MemberResolver normalMemberResolver = (attributeContext, metadataContext) -> {
		final ManagedDomainType<?> ownerType = attributeContext.getOwnerType();
		final Property property = attributeContext.getPropertyMapping();
		final Type.PersistenceType persistenceType = ownerType.getPersistenceType();
		if ( Type.PersistenceType.EMBEDDABLE == persistenceType ) {
			return embeddedMemberResolver.resolveMember( attributeContext, metadataContext );
		}
		else if ( Type.PersistenceType.ENTITY == persistenceType
				|| Type.PersistenceType.MAPPED_SUPERCLASS == persistenceType ) {
			final AbstractIdentifiableType<?> identifiableType = (AbstractIdentifiableType<?>) ownerType;
			final EntityPersister declaringEntityPersister = getDeclaringEntity( identifiableType, metadataContext );
			final String propertyName = property.getName();

			final AttributeMapping attributeMapping = declaringEntityPersister.findAttributeMapping( propertyName );
			if ( attributeMapping == null ) {
				// just like in #determineIdentifierJavaMember , this *should* indicate we have an IdClass mapping
				return virtualIdentifierMemberResolver.resolveMember( attributeContext, metadataContext );
			}
			else {
				final Getter getter = getter( declaringEntityPersister, property );
				return getter instanceof PropertyAccessMapImpl.GetterImpl
						? new MapMember( propertyName, property.getType().getReturnedClass() )
						: getter.getMember();
			}
		}
		else {
			throw new IllegalArgumentException( "Unexpected owner type : " + persistenceType );
		}
	};

	private final MemberResolver identifierMemberResolver = (attributeContext, metadataContext) -> {
		final AbstractIdentifiableType<?> identifiableType =
				(AbstractIdentifiableType<?>) attributeContext.getOwnerType();
		final EntityPersister declaringEntityMapping = getDeclaringEntity( identifiableType, metadataContext );
		final EntityIdentifierMapping identifierMapping = declaringEntityMapping.getIdentifierMapping();
		final Property propertyMapping = attributeContext.getPropertyMapping();
		if ( !propertyMapping.getName().equals( identifierMapping.getAttributeName() ) ) {
			// this *should* indicate processing part of an IdClass...
			return virtualIdentifierMemberResolver.resolveMember( attributeContext, metadataContext );
		}

		final Getter getter = getter( declaringEntityMapping, propertyMapping );
		if ( getter instanceof PropertyAccessMapImpl.GetterImpl ) {
			return new MapMember( identifierMapping.getAttributeName(), identifierMapping.getJavaType().getJavaTypeClass() );
		}
		else {
			return getter.getMember();
		}
	};

	private final MemberResolver versionMemberResolver = (attributeContext, metadataContext) -> {
		final AbstractIdentifiableType<?> identifiableType =
				(AbstractIdentifiableType<?>) attributeContext.getOwnerType();
		final EntityPersister entityPersister = getDeclaringEntity( identifiableType, metadataContext );
		final EntityVersionMapping versionMapping = entityPersister.getVersionMapping();
		assert entityPersister.isVersioned();
		assert versionMapping != null;

		final String versionPropertyName = attributeContext.getPropertyMapping().getName();
		if ( !versionPropertyName.equals( versionMapping.getVersionAttribute().getAttributeName() ) ) {
			// this should never happen, but to be safe...
			throw new IllegalArgumentException( "Given property did not match declared version property" );
		}

		final Getter getter = getter( entityPersister, attributeContext.getPropertyMapping() );
		if ( getter instanceof PropertyAccessMapImpl.GetterImpl ) {
			return new MapMember( versionPropertyName, versionMapping.getJavaType().getJavaTypeClass() );
		}
		else {
			return getter.getMember();
		}
	};

	private static Getter getter(EntityPersister declaringEntityMapping, Property propertyMapping) {
		return declaringEntityMapping.getRepresentationStrategy()
				.resolvePropertyAccess( propertyMapping )
				.getGetter();
	}

}
