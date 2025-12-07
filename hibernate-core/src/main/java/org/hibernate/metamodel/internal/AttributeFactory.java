/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.ValueClassification;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.model.domain.internal.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
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
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.domain.SqmMappedSuperclassDomainType;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.EntityType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;
import org.hibernate.type.spi.CompositeTypeImplementor;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.Attribute;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

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
	 *
	 * @return The built attribute descriptor or null if the attribute is not part of the JPA 2 model (eg backrefs)
	 */
	public <X> PersistentAttribute<X, ?> buildAttribute(ManagedDomainType<X> ownerType, Property property) {
		return buildAttribute( ownerType, property, context );
	}

	public static <X> PersistentAttribute<X, ?> buildAttribute(
			ManagedDomainType<X> ownerType,
			Property property,
			MetadataContext metadataContext) {
		if ( property.isSynthetic() ) {
			// hide synthetic/virtual properties (fabricated by Hibernate) from the JPA metamodel.
			CORE_LOGGER.tracef( "Skipping synthetic property %s(%s)", ownerType.getTypeName(), property.getName() );
			return null;
		}

		CORE_LOGGER.tracef( "Building attribute [%s.%s]", ownerType.getTypeName(), property.getName() );
		final var attributeMetadata =
				determineAttributeMetadata( wrap( ownerType, property ), normalMemberResolver, metadataContext );

		if ( attributeMetadata instanceof PluralAttributeMetadata ) {
			return PluralAttributeBuilder.build(
					(PluralAttributeMetadata<X,?,?>) attributeMetadata,
					property.isGeneric(),
					metadataContext
			);
		}

		final var singularAttributeMetadata = (SingularAttributeMetadata<X, ?>) attributeMetadata;
		final var valueContext = singularAttributeMetadata.getValueContext();
		final var domainType = (SqmDomainType<?>) determineSimpleType( valueContext, metadataContext );
		final var relationalJavaType = determineRelationalJavaType( valueContext, domainType, metadataContext );
		return new SingularAttributeImpl<>(
				ownerType,
				attributeMetadata.getName(),
				attributeMetadata.getAttributeClassification(),
				domainType,
				relationalJavaType,
				attributeMetadata.getMember(),
				false,
				false,
				property.isOptional(),
				property.isGeneric()
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
	 *
	 * @return The built attribute descriptor
	 */
	public <X> SingularPersistentAttribute<X, ?> buildIdAttribute(
			IdentifiableDomainType<X> ownerType,
			Property property) {
		CORE_LOGGER.tracef( "Building identifier attribute [%s.%s]", ownerType.getTypeName(), property.getName() );

		final var attributeMetadata =
				determineAttributeMetadata( wrap( ownerType, property ), identifierMemberResolver );
		final var singularAttributeMetadata = (SingularAttributeMetadata<X, ?>) attributeMetadata;
		final var domainType = (SqmDomainType<?>) determineSimpleType( singularAttributeMetadata.getValueContext() );
		return new SingularAttributeImpl.Identifier<>(
				ownerType,
				property.getName(),
				domainType,
				attributeMetadata.getMember(),
				attributeMetadata.getAttributeClassification(),
				property.isGeneric()
		);
	}

	/**
	 * Build the version attribute descriptor
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param property The Hibernate property descriptor for the version attribute
	 * @param <X> The type of the owner
	 *
	 * @return The built attribute descriptor
	 */
	public <X> SingularAttributeImpl<X, ?> buildVersionAttribute(
			IdentifiableDomainType<X> ownerType,
			Property property) {
		CORE_LOGGER.tracef( "Building version attribute [%s.%s]", ownerType.getTypeName(), property.getName() );

		final var attributeMetadata =
				determineAttributeMetadata( wrap( ownerType, property ), versionMemberResolver );
		final var singularAttributeMetadata = (SingularAttributeMetadata<X, ?>) attributeMetadata;
		final var domainType = (SqmDomainType<?>) determineSimpleType( singularAttributeMetadata.getValueContext() );
		return new SingularAttributeImpl.Version<>(
				ownerType,
				property.getName(),
				attributeMetadata.getAttributeClassification(),
				domainType,
				attributeMetadata.getMember()
		);
	}

	private DomainType<?> determineSimpleType(ValueContext typeContext) {
		return determineSimpleType( typeContext, context );
	}

	public static DomainType<?> determineSimpleType(ValueContext typeContext, MetadataContext context) {
		return switch ( typeContext.getValueClassification() ) {
			case BASIC -> basicDomainType( typeContext, context );
			case ENTITY -> entityDomainType( typeContext, context );
			case EMBEDDABLE -> embeddableDomainType( typeContext, context );
			default -> throw new AssertionFailure( "Unknown type : " + typeContext.getValueClassification() );
		};
	}

	private static EmbeddableDomainType<?> embeddableDomainType(ValueContext typeContext, MetadataContext context) {
		final var component = (Component) typeContext.getHibernateValue();
		return component.isDynamic()
				? dynamicEmbeddableType( context, component )
				: classEmbeddableType( context, component ); // we should have a non-dynamic embeddable
	}

	private static EmbeddableDomainType<?> classEmbeddableType(MetadataContext context, Component component) {
		assert component.getComponentClassName() != null;
		final var embeddableClass = component.getComponentClass();

		if ( !component.isGeneric() ) {
			final var cached = context.locateEmbeddable( embeddableClass, component );
			if ( cached != null ) {
				return cached;
			}
		}

		final var mappedSuperclass = component.getMappedSuperclass();
		final var superType =
				mappedSuperclass == null
						? null
						: context.locateMappedSuperclassType( mappedSuperclass );

		final var discriminatorType = component.isPolymorphic() ? component.getDiscriminatorType() : null;

		final var embeddableType = embeddableType( context, embeddableClass, superType, discriminatorType );
		context.registerEmbeddableType( embeddableType, component );

		if ( component.isPolymorphic() ) {
			final var embeddableSubclasses = component.getDiscriminatorValues().values();
			final java.util.Map<String, EmbeddableTypeImpl<?>> domainTypes = new HashMap<>();
			domainTypes.put( embeddableType.getTypeName(), embeddableType );
			final var classLoaderService =
					context.getRuntimeModelCreationContext().getBootstrapContext().getClassLoaderService();
			for ( final String subclassName : embeddableSubclasses ) {
				if ( domainTypes.containsKey( subclassName ) ) {
					assert subclassName.equals( embeddableType.getTypeName() );
				}
				else {
					final Class<?> subclass = classLoaderService.classForName( subclassName );
					final var superTypeEmbeddable = domainTypes.get( component.getSuperclass( subclassName ) );
					final var subType = embeddableType( context, subclass, superTypeEmbeddable, discriminatorType );
					domainTypes.put( subclassName, subType );
					context.registerEmbeddableType( subType, component );
				}
			}
		}

		return embeddableType;
	}

	private static <J> EmbeddableTypeImpl<J> embeddableType(
			MetadataContext context,
			Class<J> subclass,
			ManagedDomainType<?> superType,
			DiscriminatorType<?> discriminatorType) {
		@SuppressWarnings("unchecked")
		final var castSuperType = (ManagedDomainType<? super J>) superType;
		return new EmbeddableTypeImpl<>(
				context.getJavaTypeRegistry().resolveManagedTypeDescriptor( subclass ),
				castSuperType,
				discriminatorType,
				false,
				context.getJpaMetamodel()
		);
	}

	private static EmbeddableTypeImpl<?> dynamicEmbeddableType(MetadataContext context, Component component) {
		final var embeddableType = new EmbeddableTypeImpl<>(
				context.getJavaTypeRegistry().resolveDescriptor( java.util.Map.class ),
				null,
				null,
				true,
				context.getJpaMetamodel()
		);

		context.registerComponentByEmbeddable( embeddableType, component);

		final var inFlightAccess = embeddableType.getInFlightAccess();
		for ( Property property : component.getProperties() ) {
			final var attribute = buildAttribute( embeddableType, property, context);
			if ( attribute != null ) {
				inFlightAccess.addAttribute( attribute );
			}
		}
		inFlightAccess.finishUp();

		return embeddableType;
	}

	private static DomainType<?> entityDomainType(ValueContext typeContext, MetadataContext context) {
		final var type = typeContext.getHibernateValue().getType();
		if ( type instanceof EntityType entityType ) {
			final var domainType = context.locateIdentifiableType( entityType.getAssociatedEntityName() );
			if ( domainType == null ) {
				// Due to the use of generics, it can happen that a mapped super class uses a type
				// for an attribute that is not a managed type. Since this case is not specifically mentioned
				// in the Jakarta Persistence spec, we handle this by returning a "dummy" entity type
				final var domainJavaType =
						context.getJavaTypeRegistry()
								.resolveDescriptor( typeContext.getJpaBindableType() );
				return new EntityTypeImpl<>( domainJavaType, context.getJpaMetamodel() );
			}
			else {
				return domainType;
			}
		}

		final AnyType anyType = (AnyType) type;
		return new AnyMappingDomainTypeImpl<>(
				(Any) typeContext.getHibernateValue(),
				anyType,
				context.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( anyType.getReturnedClass() ),
				context.getRuntimeModelCreationContext().getSessionFactory().getMappingMetamodel()
		);
	}

	private static DomainType<?> basicDomainType(ValueContext typeContext, MetadataContext context) {
		final Value hibernateValue = typeContext.getHibernateValue();
		if ( typeContext.getJpaBindableType().isPrimitive()
				&& ( (SimpleValue) hibernateValue ).getJpaAttributeConverterDescriptor() == null ) {
			// Special BasicDomainType necessary for primitive types in the JPA metamodel.
			// When a converted is applied to the attribute we already resolve to the correct type
			final var type = typeContext.getJpaBindableType();
			return context.resolveBasicType( type );
		}
		else {
			final var type = hibernateValue.getType();
			if ( type instanceof BasicPluralType<?, ?> pluralType ) {
				if ( pluralType.getElementType().getJavaTypeDescriptor()
						instanceof EmbeddableAggregateJavaType<?> ) {
					final var aggregateColumn = (AggregateColumn) hibernateValue.getColumns().get( 0 );
					classEmbeddableType( context, aggregateColumn.getComponent() );
				}
			}
			return (DomainType<?>) type;
		}
	}

	private static JavaType<?> determineRelationalJavaType(
			ValueContext typeContext,
			DomainType<?> metaModelType,
			MetadataContext context) {
		if ( typeContext.getValueClassification() == ValueClassification.BASIC ) {
			final var value = (SimpleValue) typeContext.getHibernateValue();
			final var descriptor = value.getJpaAttributeConverterDescriptor();
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
		final var metamodel = metadataContext.getMetamodel();
		final var persistenceType = ownerType.getPersistenceType();
		return switch ( persistenceType ) {
			case ENTITY -> metamodel.getEntityDescriptor( ownerType.getTypeName() );
			case MAPPED_SUPERCLASS -> {
				final var ownerAsSuper = (MappedSuperclassTypeImpl<?>) ownerType;
				final var persistentClass = metadataContext.getPersistentClassHostingProperties( ownerAsSuper );
				yield persistentClass == null ? null : metamodel.findEntityDescriptor( persistentClass.getClassName() );
			}
			default -> throw new AssertionFailure( "Cannot get the metamodel for PersistenceType: " + persistenceType );
		};
	}

	/**
	 * Here is most of the nuts and bolts of this factory, where we interpret the known JPA metadata
	 * against the known Hibernate metadata and build a descriptor for the attribute.
	 *
	 * @param attributeContext The attribute to be described
	 * @param memberResolver Strategy for how to resolve the member defining the attribute.
	 * @param <X> The owner type
	 *
	 * @return The attribute description
	 */
	private <X> AttributeMetadata<X, ?> determineAttributeMetadata(
			AttributeContext<X> attributeContext,
			MemberResolver memberResolver) {
		return determineAttributeMetadata( attributeContext, memberResolver, context );
	}

	private static <X> AttributeMetadata<X, ?> determineAttributeMetadata(
			AttributeContext<X> attributeContext,
			MemberResolver memberResolver,
			MetadataContext context) {
		final Property propertyMapping = attributeContext.getPropertyMapping();
		final String propertyName = propertyMapping.getName();

		CORE_LOGGER.tracef( "Starting attribute metadata determination [%s]", propertyName );

		final var member = memberResolver.resolveMember( attributeContext, context );
		CORE_LOGGER.tracef( "\tMember: %s", member );

		final var value = propertyMapping.getValue();
		final var type = value.getType();
		CORE_LOGGER.tracef( "\tType: %s [%s]", type.getName(), type.getClass().getSimpleName() );

		if ( type instanceof AnyType ) {
			return new SingularAttributeMetadataImpl<>(
					propertyMapping,
					attributeContext.getOwnerType(),
					member,
					AttributeClassification.ANY
			);
		}
		else if ( type instanceof EntityType ) {
			// entity
			return new SingularAttributeMetadataImpl<>(
					propertyMapping,
					attributeContext.getOwnerType(),
					member,
					determineSingularAssociationClassification( member )
			);
		}
		else if ( type instanceof CollectionType ) {
			// collection
			if ( value instanceof Collection collection ) {
				final var elementType = collection.getElement().getType();
				final boolean isManyToMany = isManyToMany( member );
				return new PluralAttributeMetadataImpl<>(
						propertyMapping,
						attributeContext.getOwnerType(),
						member,
						collectionClassification( elementType, isManyToMany ),
						elementClassification( elementType, isManyToMany ),
						indexClassification( value )
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
		else if ( type instanceof ComponentType ) {
			// component
			return new SingularAttributeMetadataImpl<>(
					propertyMapping,
					attributeContext.getOwnerType(),
					member,
					AttributeClassification.EMBEDDED
			);
		}
		else {
			assert type instanceof BasicType<?>;
			// basic type
			return new SingularAttributeMetadataImpl<>(
					propertyMapping,
					attributeContext.getOwnerType(),
					member,
					AttributeClassification.BASIC
			);
		}
		throw new UnsupportedMappingException( "oops, we are missing something: " + propertyMapping );
	}

	private static AttributeClassification indexClassification(Value value) {
		if ( value instanceof Map map ) {
			return keyClassification( map.getIndex().getType() );
		}
		else if ( value instanceof List ) {
			return AttributeClassification.BASIC;
		}
		else {
			return null;
		}
	}

	private static AttributeClassification elementClassification(
			org.hibernate.type.Type elementType, boolean isManyToMany) {
		// First, determine the type of the elements and use that to help determine the
		// collection type
		if ( elementType instanceof AnyType ) {
			return AttributeClassification.ANY;
		}
		else if ( elementType instanceof ComponentType ) {
			return AttributeClassification.EMBEDDED;
		}
		else if ( elementType instanceof EntityType ) {
			return isManyToMany
					? AttributeClassification.MANY_TO_MANY
					: AttributeClassification.ONE_TO_MANY;
		}
		else {
			return AttributeClassification.BASIC;
		}
	}

	private static AttributeClassification collectionClassification(
			org.hibernate.type.Type elementType, boolean isManyToMany) {
		if ( elementType instanceof EntityType ) {
			return isManyToMany
					? AttributeClassification.MANY_TO_MANY
					: AttributeClassification.ONE_TO_MANY;
		}
		else {
			return AttributeClassification.ELEMENT_COLLECTION;
		}
	}

	private static AttributeClassification keyClassification(org.hibernate.type.Type keyType) {
		if ( keyType instanceof AnyType ) {
			return AttributeClassification.ANY;
		}
		else if ( keyType instanceof ComponentType ) {
			return AttributeClassification.EMBEDDED;
		}
		else if ( keyType instanceof EntityType ) {
			return AttributeClassification.MANY_TO_ONE;
		}
		else {
			return AttributeClassification.BASIC;
		}
	}

	public static AttributeClassification determineSingularAssociationClassification(Member member) {
		if ( member instanceof Field field ) {
			return field.getAnnotation( OneToOne.class ) != null
					? AttributeClassification.ONE_TO_ONE
					: AttributeClassification.MANY_TO_ONE;
		}
		else if ( member instanceof MapMember ) {
			return AttributeClassification.MANY_TO_ONE; // curious to see how this works for non-annotated methods
		}
		else if ( member instanceof Method method) {
			return method.getAnnotation( OneToOne.class ) != null
					? AttributeClassification.ONE_TO_ONE
					: AttributeClassification.MANY_TO_ONE;
		}
		else {
			throw new AssertionFailure( "Unexpected member type" );
		}
	}

	public static ParameterizedType getSignatureType(Member member) {
		final java.lang.reflect.Type type;
		if ( member instanceof Field field ) {
			type = field.getGenericType();
		}
		else if ( member instanceof Method method ) {
			type = method.getGenericReturnType();
		}
		else if ( member instanceof MapMember mapMember ) {
			type = mapMember.getType();
		}
		else {
			throw new AssertionFailure( "Unexpected member type" );
		}
		//this is a raw type
		return type instanceof Class ? null : (ParameterizedType) type;
	}

	public static boolean isManyToMany(Member member) {
		if ( member instanceof Field field ) {
			return field.getAnnotation( ManyToMany.class ) != null;
		}
		else if ( member instanceof Method method ) {
			return method.getAnnotation( ManyToMany.class ) != null;
		}
		else {
			return false;
		}
	}

	private static final MemberResolver embeddedMemberResolver = (attributeContext, metadataContext) -> {
		// the owner is an embeddable
		final var ownerType = (EmbeddableDomainType<?>) attributeContext.getOwnerType();
		return resolveEmbeddedMember( attributeContext.getPropertyMapping(), ownerType, metadataContext );
	};

	private static Member resolveEmbeddedMember(
			Property property,
			EmbeddableDomainType<?> ownerType,
			MetadataContext metadataContext) {
		final Component embeddable = metadataContext.getEmbeddableBootDescriptor( ownerType );
		final var ownerComponentType = (CompositeTypeImplementor) embeddable.getType();
		final var ownerMappingModel = ownerComponentType.getMappingModelPart();
		final var ownerRepStrategy = ownerRepresentationStrategy( metadataContext, ownerMappingModel, embeddable );
		return ownerRepStrategy.getMode() == RepresentationMode.MAP
				? new MapMember( property.getName(), property.getType().getReturnedClass() )
				: ownerRepStrategy.resolvePropertyAccess( property ).getGetter().getMember();
	}

	private static EmbeddableRepresentationStrategy ownerRepresentationStrategy(
			MetadataContext metadataContext,
			EmbeddableValuedModelPart ownerMappingModelDescriptor,
			Component ownerBootDescriptor) {
		if ( ownerMappingModelDescriptor == null ) {
			// When an entity uses a type variable, bound by a mapped superclass, for an embedded id,
			// we will not create a model part for the component, but we still need the representation strategy here,
			// in order to discover the property members to expose on the JPA metamodel
			return ownerBootDescriptor.getBuildingContext().getBootstrapContext()
							.getRepresentationStrategySelector()
							.resolveStrategy( ownerBootDescriptor, null,
									metadataContext.getRuntimeModelCreationContext() );
		}
		else {
			return ownerMappingModelDescriptor.getEmbeddableTypeDescriptor()
					.getRepresentationStrategy();
		}
	}


	private static final MemberResolver virtualIdentifierMemberResolver = (attributeContext, metadataContext) -> {
		final var identifiableType = (AbstractIdentifiableType<?>) attributeContext.getOwnerType();
		final var declaringEntity = getDeclaringEntity( identifiableType, metadataContext );
		return resolveVirtualIdentifierMember( attributeContext.getPropertyMapping(), declaringEntity );
	};

	private static Member resolveVirtualIdentifierMember( Property property, EntityPersister entityPersister) {
		final var identifierMapping = entityPersister.getIdentifierMapping();
		if ( identifierMapping.getNature() != EntityIdentifierMapping.Nature.VIRTUAL ) {
			throw new IllegalArgumentException( "expecting IdClass mapping" );
		}

		final var cid = (CompositeIdentifierMapping) identifierMapping;
		final var embeddable = cid.getPartMappingType();
		final String attributeName = property.getName();
		final var attributeMapping = embeddable.findAttributeMapping( attributeName );
		if ( attributeMapping == null ) {
			throw new PropertyNotFoundException(
					"Could not resolve attribute '" + attributeName
							+ "' of '" + embeddable.getJavaType().getJavaTypeClass().getName() + "'"
			);
		}

		final Getter getter = attributeMapping.getPropertyAccess().getGetter();
		return getter instanceof PropertyAccessMapImpl.GetterImpl
				? new MapMember( attributeName, property.getType().getReturnedClass() )
				: getter.getMember();
	}

	/**
	 * A {@link Member} resolver for normal attributes.
	 */
	private static final MemberResolver normalMemberResolver = (attributeContext, metadataContext) -> {
		final var ownerType = attributeContext.getOwnerType();
		final Property property = attributeContext.getPropertyMapping();
		final var persistenceType = ownerType.getPersistenceType();
		return switch ( persistenceType ) {
			case ENTITY ->
					resolveEntityMember( property,
							getDeclaringEntity( (AbstractIdentifiableType<?>) ownerType, metadataContext ) );
			case MAPPED_SUPERCLASS ->
					resolveMappedSuperclassMember( property, (MappedSuperclassDomainType<?>) ownerType, metadataContext );
			case EMBEDDABLE ->
					embeddedMemberResolver.resolveMember( attributeContext, metadataContext );
			default -> throw new IllegalArgumentException( "Unexpected owner type : " + persistenceType );
		};
	};

	private static Member resolveEntityMember(Property property, EntityPersister declaringEntity) {
		final String propertyName = property.getName();
		return !propertyName.equals( declaringEntity.getIdentifierPropertyName() )
			&& declaringEntity.findAttributeMapping( propertyName ) == null
				// just like in #determineIdentifierJavaMember , this *should* indicate we have an IdClass mapping
				? resolveVirtualIdentifierMember( property, declaringEntity )
				: getter( declaringEntity, property, propertyName, property.getType().getReturnedClass() );
	}

	private static Member resolveMappedSuperclassMember(
			Property property,
			MappedSuperclassDomainType<?> ownerType,
			MetadataContext context) {
		return property.getGetter( ownerType.getJavaType() ).getMember();
//		final EntityPersister declaringEntity =
//				getDeclaringEntity( (AbstractIdentifiableType<?>) ownerType, context );
//		if ( declaringEntity != null ) {
//			return resolveEntityMember( property, declaringEntity );
//		}
//		else {
//			final ManagedDomainType<?> subType = ownerType.getSubTypes().iterator().next();
//			final Type.PersistenceType persistenceType = subType.getPersistenceType();
//			return switch ( persistenceType ) {
//				case ENTITY ->
//						resolveEntityMember( property,
//								getDeclaringEntity( (AbstractIdentifiableType<?>) subType, context ) );
//				case MAPPED_SUPERCLASS ->
//						resolveMappedSuperclassMember( property, (MappedSuperclassDomainType<?>) subType, context );
//				case EMBEDDABLE ->
//						resolveEmbeddedMember( property, (EmbeddableDomainType<?>) subType, context );
//				default -> throw new IllegalArgumentException( "Unexpected PersistenceType: " + persistenceType );
//			};
//		}
	}

	private final MemberResolver identifierMemberResolver = (attributeContext, metadataContext) -> {
		final var identifiableType = (AbstractIdentifiableType<?>) attributeContext.getOwnerType();
		if ( identifiableType instanceof SqmMappedSuperclassDomainType<?> ) {
			return attributeContext.getPropertyMapping()
					.getGetter( identifiableType.getJavaType() )
					.getMember();
		}
		else {
			final var declaringEntityMapping = getDeclaringEntity( identifiableType, metadataContext );
			final var identifierMapping = declaringEntityMapping.getIdentifierMapping();
			final Property propertyMapping = attributeContext.getPropertyMapping();
			return !propertyMapping.getName().equals( identifierMapping.getAttributeName() )
					// this *should* indicate processing part of an IdClass
					? virtualIdentifierMemberResolver.resolveMember( attributeContext, metadataContext )
					: getter( declaringEntityMapping, propertyMapping,
							identifierMapping.getAttributeName(), identifierMapping.getJavaType().getJavaTypeClass() );
		}
	};

	private final MemberResolver versionMemberResolver = (attributeContext, metadataContext) -> {
		final var identifiableType = (AbstractIdentifiableType<?>) attributeContext.getOwnerType();
		if ( identifiableType instanceof SqmMappedSuperclassDomainType<?> ) {
			return attributeContext.getPropertyMapping()
					.getGetter( identifiableType.getJavaType() )
					.getMember();
		}
		else {
			final var entityPersister = getDeclaringEntity( identifiableType, metadataContext );
			final var versionMapping = entityPersister.getVersionMapping();
			assert entityPersister.isVersioned();
			assert versionMapping != null;

			final String versionPropertyName = attributeContext.getPropertyMapping().getName();
			if ( !versionPropertyName.equals( versionMapping.getVersionAttribute().getAttributeName() ) ) {
				// this should never happen but check it to be safe
				throw new IllegalArgumentException( "Given property did not match declared version property" );
			}
			return getter( entityPersister, attributeContext.getPropertyMapping(),
					versionPropertyName, versionMapping.getJavaType().getJavaTypeClass() );
		}
	};

	private static Member getter(EntityPersister persister, Property property, String name, Class<?> type) {
		final Getter getter = getter( persister, property );
		return getter instanceof PropertyAccessMapImpl.GetterImpl
				? new MapMember( name, type )
				: getter.getMember();
	}

	private static Getter getter(EntityPersister persister, Property property) {
		return persister.getRepresentationStrategy()
				.resolvePropertyAccess( property )
				.getGetter();
	}
}
