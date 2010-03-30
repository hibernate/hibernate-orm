/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.metamodel;

import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.IdentifiableType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.EntityMode;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.ComponentType;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.EntityType;

/**
 * A factory for building {@link Attribute} instances.  Exposes 3 main services for building<ol>
 * <li>{@link #buildAttribute normal attributes}</li>
 * <li>{@link #buildIdAttribute id attributes}</li>
 * <li>{@link #buildVersionAttribute version attributes}</li>
 * <ol>
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class AttributeFactory {
	private static final Logger log = LoggerFactory.getLogger( AttributeFactory.class );

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
	 * @return The built attribute descriptor or null if the attribute is not part of the JPA 2 model (eg backrefs)
	 */
	@SuppressWarnings({ "unchecked" })
	public <X, Y> AttributeImplementor<X, Y> buildAttribute(AbstractManagedType<X> ownerType, Property property) {
		if ( property.isSynthetic() ) {
			// hide synthetic/virtual properties (fabricated by Hibernate) from the JPA metamodel.
			log.trace(  "Skipping synthetic property {}({})", ownerType.getJavaType().getName(), property.getName() );
			return null;
		}
		log.trace( "Building attribute [{}.{}]", ownerType.getJavaType().getName(), property.getName() );
		final AttributeContext<X> attributeContext = wrap( ownerType, property );
		final AttributeMetadata<X,Y> attributeMetadata =
				determineAttributeMetadata( attributeContext, NORMAL_MEMBER_RESOLVER );

		if ( attributeMetadata.isPlural() ) {
			return buildPluralAttribute( (PluralAttributeMetadata) attributeMetadata );
		}
		else {
			final SingularAttributeMetadata<X,Y> singularAttributeMetadata =
					(SingularAttributeMetadata<X,Y>) attributeMetadata;
			final Type<Y> metaModelType = getMetaModelType( singularAttributeMetadata.getValueContext() );
			return new SingularAttributeImpl<X,Y>(
					attributeMetadata.getName(),
					attributeMetadata.getJavaType(),
					ownerType,
					attributeMetadata.getMember(),
					false,
					false,
					property.isOptional(),
					metaModelType,
					attributeMetadata.getPersistentAttributeType()
			);
		}
	}

	private <X> AttributeContext<X> wrap(final AbstractManagedType<X> ownerType, final Property property) {
		return new AttributeContext<X>() {
			public AbstractManagedType<X> getOwnerType() {
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
	 * @return The built attribute descriptor
	 */
	@SuppressWarnings({ "unchecked" })
	public <X, Y> SingularAttributeImpl<X, Y> buildIdAttribute(AbstractIdentifiableType<X> ownerType, Property property) {
		log.trace( "Building identifier attribute [{}.{}]", ownerType.getJavaType().getName(), property.getName() );
		final AttributeContext<X> attributeContext = wrap( ownerType, property );
		final SingularAttributeMetadata<X,Y> attributeMetadata =
				(SingularAttributeMetadata<X, Y>) determineAttributeMetadata( attributeContext, IDENTIFIER_MEMBER_RESOLVER );
		final Type<Y> metaModelType = getMetaModelType( attributeMetadata.getValueContext() );
		return new SingularAttributeImpl.Identifier(
				property.getName(),
				attributeMetadata.getJavaType(),
				ownerType,
				attributeMetadata.getMember(),
				metaModelType,
				attributeMetadata.getPersistentAttributeType()
		);
	}

	/**
	 * Build the version attribute descriptor
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param property The Hibernate property descriptor for the version attribute
	 * @param <X> The type of the owner
	 * @param <Y> The attribute type
	 * @return The built attribute descriptor
	 */
	@SuppressWarnings({ "unchecked" })
	public <X, Y> SingularAttributeImpl<X, Y> buildVersionAttribute(AbstractIdentifiableType<X> ownerType, Property property) {
		log.trace( "Building version attribute [{}.{}]", ownerType.getJavaType().getName(), property.getName() );
		final AttributeContext<X> attributeContext = wrap( ownerType, property );
		final SingularAttributeMetadata<X,Y> attributeMetadata =
				(SingularAttributeMetadata<X, Y>) determineAttributeMetadata( attributeContext, VERSION_MEMBER_RESOLVER );
		final Type<Y> metaModelType = getMetaModelType( attributeMetadata.getValueContext() );
		return new SingularAttributeImpl.Version(
				property.getName(),
				attributeMetadata.getJavaType(),
				ownerType,
				attributeMetadata.getMember(),
				metaModelType,
				attributeMetadata.getPersistentAttributeType()
		);
	}

	@SuppressWarnings( "unchecked" )
	private <X, Y, E, K> AttributeImplementor<X, Y> buildPluralAttribute(PluralAttributeMetadata<X,Y,E> attributeMetadata) {
		final Type<E> elementType = getMetaModelType( attributeMetadata.getElementValueContext() );
		if ( java.util.Map.class.isAssignableFrom( attributeMetadata.getJavaType() ) ) {
			final Type<K> keyType = getMetaModelType( attributeMetadata.getMapKeyValueContext() );
			return PluralAttributeImpl.create( attributeMetadata.getOwnerType(), elementType, attributeMetadata.getJavaType(), keyType )
					.member( attributeMetadata.getMember() )
					.property( attributeMetadata.getPropertyMapping() )
					.persistentAttributeType( attributeMetadata.getPersistentAttributeType() )
					.build();
		}
		else {
			return PluralAttributeImpl.create( attributeMetadata.getOwnerType(), elementType, attributeMetadata.getJavaType(), null )
					.member( attributeMetadata.getMember() )
					.property( attributeMetadata.getPropertyMapping() )
					.persistentAttributeType( attributeMetadata.getPersistentAttributeType() )
					.build();
		}
	}

	@SuppressWarnings( "unchecked" )
	private <Y> Type<Y> getMetaModelType(ValueContext typeContext) {
		switch ( typeContext.getValueClassification() ) {
			case BASIC: {
				return new BasicTypeImpl<Y>(
						typeContext.getBindableType(),
						Type.PersistenceType.BASIC
				);
			}
			case ENTITY: {
				final org.hibernate.type.EntityType type = (EntityType) typeContext.getValue().getType();
				return (Type<Y>) context.locateEntityType( type.getAssociatedEntityName() );
			}
			case EMBEDDABLE: {
				final Component component = (Component) typeContext.getValue();
				final EmbeddableTypeImpl<Y> embeddableType = new EmbeddableTypeImpl<Y>(
						typeContext.getBindableType(),
						typeContext.getAttributeMetadata().getOwnerType(),
						(ComponentType) typeContext.getValue().getType()
				);
				context.registerEmbeddedableType( embeddableType );
				final Iterator<Property> subProperties = component.getPropertyIterator();
				while ( subProperties.hasNext() ) {
					final Property property = subProperties.next();
					final AttributeImplementor<Y, Object> attribute = buildAttribute( embeddableType, property );
					if ( attribute != null ) {
						embeddableType.getBuilder().addAttribute( attribute );
					}
				}
				embeddableType.lock();
				return embeddableType;
			}
			default: {
				throw new AssertionFailure( "Unknown type : " + typeContext.getValueClassification() );
			}
		}
	}

	private EntityMetamodel getDeclarerEntityMetamodel(IdentifiableType<?> ownerType) {
		final Type.PersistenceType persistenceType = ownerType.getPersistenceType();
		if ( persistenceType == Type.PersistenceType.ENTITY) {
			return context.getSessionFactory()
					.getEntityPersister( ownerType.getJavaType().getName() )
					.getEntityMetamodel();
		}
		else if ( persistenceType == Type.PersistenceType.MAPPED_SUPERCLASS) {
			PersistentClass persistentClass =
					context.getPersistentClassHostingProperties( (MappedSuperclassTypeImpl<?>) ownerType );
			return context.getSessionFactory()
				.getEntityPersister( persistentClass.getClassName() )
				.getEntityMetamodel();
		}
		else {
			throw new AssertionFailure( "Cannot get the metamodel for PersistenceType: " + persistenceType );
		}
	}

	/**
	 * A contract for defining the meta information about a {@link Value}
	 */
	private interface ValueContext {
		/**
		 * Enum of the simplified types a value might be.  These relate more to the Hibernate classification
		 * then the JPA classification
		 */
		enum ValueClassification {
			EMBEDDABLE,
			ENTITY,
			BASIC
		}

		/**
		 * Retrieve the value itself
		 *
		 * @return The value
		 */
		public Value getValue();

		public Class getBindableType();

		/**
		 * Retrieve the simplified value classification
		 *
		 * @return The value type
		 */
		public ValueClassification getValueClassification();

		/**
		 * Retrieve the metadata about the attribute from which this value comes
		 *
		 * @return The "containing" attribute metadata.
		 */
		public AttributeMetadata getAttributeMetadata();
	}

	/**
	 * Basic contract for describing an attribute.  The "description" is partially in terms
	 * of JPA ({@link #getPersistentAttributeType} and {@link #getOwnerType}), partially in
	 * terms of Hibernate ({@link #getPropertyMapping}) and partially just in terms of the java
	 * model itself ({@link #getName}, {@link #getMember} and {@link #getJavaType}).
	 *
	 * @param <X> The attribute owner type
	 * @param <Y> The attribute type.
	 */
	private interface AttributeMetadata<X,Y> {
		/**
		 * Retrieve the name of the attribute
		 *
		 * @return The attribute name
		 */
		public String getName();

		/**
		 * Retrieve the member defining the attribute
		 *
		 * @return The attribute member
		 */
		public Member getMember();

		/**
		 * Retrieve the attribute java type.
		 *
		 * @return The java type of the attribute.
		 */
		public Class<Y> getJavaType();

		/**
		 * Get the JPA attribute type classification for this attribute.
		 *
		 * @return The JPA attribute type classification
		 */
		public Attribute.PersistentAttributeType getPersistentAttributeType();

		/**
		 * Retrieve the attribute owner's metamodel information
		 *
		 * @return The metamodel information for the attribute owner
		 */
		public AbstractManagedType<X> getOwnerType();

		/**
		 * Retrieve the Hibernate property mapping related to this attribute.
		 *
		 * @return The Hibernate property mapping
		 */
		public Property getPropertyMapping();

		/**
		 * Is the attribute plural (a collection)?
		 *
		 * @return True if it is plural, false otherwise.
		 */
		public boolean isPlural();
	}

	/**
	 * Attribute metadata contract for a non-plural attribute.
	 * @param <X> The owner type
	 * @param <Y> The attribute type
	 */
	private interface SingularAttributeMetadata<X,Y>  extends AttributeMetadata<X,Y> {
		/**
		 * Retrieve the value context for this attribute
		 *
		 * @return The attributes value context
		 */
		public ValueContext getValueContext();
	}

	/**
	 * Attribute metadata contract for a plural attribute.
	 * @param <X> The owner type
	 * @param <Y> The attribute type (the collection type)
	 * @param <E> The collection element type
	 */
	private interface PluralAttributeMetadata<X,Y,E> extends AttributeMetadata<X,Y> {
		/**
		 * Retrieve the JPA collection type classification for this attribute
		 *
		 * @return The JPA collection type classification
		 */
		public PluralAttribute.CollectionType getAttributeCollectionType();

		/**
		 * Retrieve the value context for the collection's elements.
		 *
		 * @return The value context for the collection's elements.
		 */
		public ValueContext getElementValueContext();

		/**
		 * Retrieve the value context for the collection's keys (if a map, null otherwise).
		 *
		 * @return The value context for the collection's keys (if a map, null otherwise).
		 */
		public ValueContext getMapKeyValueContext();
	}

	/**
	 * Bundle's a Hibernate property mapping together with the JPA metamodel information
	 * of the attribute owner.
	 *
	 * @param <X> The owner type.
	 */
	private interface AttributeContext<X> {
		/**
		 * Retrieve the attribute owner.
		 *
		 * @return The owner.
		 */
		public AbstractManagedType<X> getOwnerType();

		/**
		 * Retrieve the Hibernate property mapping.
		 *
		 * @return The Hibvernate property mapping.
		 */
		public Property getPropertyMapping();
	}

	/**
	 * Contract for how we resolve the {@link Member} for a give attribute context.
	 */
	private interface MemberResolver {
		public Member resolveMember(AttributeContext attributeContext);
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
	@SuppressWarnings({ "unchecked" })
	private <X,Y> AttributeMetadata<X,Y> determineAttributeMetadata(
			AttributeContext<X> attributeContext,
			MemberResolver memberResolver) {
		log.trace( "Starting attribute metadata determination [{}]", attributeContext.getPropertyMapping().getName() );
		final Member member = memberResolver.resolveMember( attributeContext );
		log.trace( "    Determined member [{}]", member );

		final Value value = attributeContext.getPropertyMapping().getValue();
		final org.hibernate.type.Type type = value.getType();
		log.trace( "    determined type [name={}, class={}]", type.getName(), type.getClass().getName() );

		if ( type.isAnyType() ) {
			throw new UnsupportedOperationException( "any not supported yet" );
		}
		else if ( type.isAssociationType() ) {
			// collection or entity
			if ( type.isEntityType() ) {
				// entity
				return new SingularAttributeMetadataImpl<X,Y>(
						attributeContext.getPropertyMapping(),
						attributeContext.getOwnerType(),
						member,
						determineSingularAssociationAttributeType( member )
				);
			}
			else {
				// collection
				if ( value instanceof Collection ) {
					final Collection collValue = (Collection) value;
					final Value elementValue = collValue.getElement();
					final org.hibernate.type.Type elementType = elementValue.getType();

					// First, determine the type of the elements and use that to help determine the
					// 		collection type)
					final Attribute.PersistentAttributeType elementPersistentAttributeType;
					final Attribute.PersistentAttributeType persistentAttributeType;
					if ( elementType.isAnyType() ) {
						throw new UnsupportedOperationException( "collection of any not supported yet" );
					}
					final boolean isManyToMany = isManyToMany( member );
					if ( elementValue instanceof Component ) {
						elementPersistentAttributeType = Attribute.PersistentAttributeType.EMBEDDED;
						persistentAttributeType = Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
					}
					else if ( elementType.isAssociationType() ) {
						elementPersistentAttributeType = isManyToMany
								? Attribute.PersistentAttributeType.MANY_TO_MANY
								: Attribute.PersistentAttributeType.ONE_TO_MANY;
						persistentAttributeType = elementPersistentAttributeType;
					}
					else {
						elementPersistentAttributeType = Attribute.PersistentAttributeType.BASIC;
						persistentAttributeType = Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
					}

					final Attribute.PersistentAttributeType keyPersistentAttributeType;

					// Finally, we determine the type of the map key (if needed)
					if ( value instanceof Map ) {
						final Value keyValue = ( ( Map ) value ).getIndex();
						final org.hibernate.type.Type keyType = keyValue.getType();

						if ( keyType.isAnyType() ) {
							throw new UnsupportedOperationException( "collection of any not supported yet" );
						}
						if ( keyValue instanceof Component ) {
							keyPersistentAttributeType = Attribute.PersistentAttributeType.EMBEDDED;
						}
						else if ( keyType.isAssociationType() ) {
							keyPersistentAttributeType = Attribute.PersistentAttributeType.MANY_TO_ONE;
						}
						else {
							keyPersistentAttributeType = Attribute.PersistentAttributeType.BASIC;
						}
					}
					else {
						keyPersistentAttributeType = null;
					}
					return new PluralAttributeMetadataImpl(
							attributeContext.getPropertyMapping(),
							attributeContext.getOwnerType(),
							member,
							persistentAttributeType,
							elementPersistentAttributeType,
							keyPersistentAttributeType
					);
				}
				else if ( value instanceof OneToMany ) {
					// TODO : is this even possible???  Really OneToMany should be describing the
					//		element value within a o.h.mapping.Collection (see logic branch above)
					throw new IllegalArgumentException( "HUH???" );
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
		}
		else if ( attributeContext.getPropertyMapping().isComposite() ) {
			// component
			return new SingularAttributeMetadataImpl<X,Y>(
					attributeContext.getPropertyMapping(),
					attributeContext.getOwnerType(),
					member,
					Attribute.PersistentAttributeType.EMBEDDED
			);
		}
		else {
			// basic type
			return new SingularAttributeMetadataImpl<X,Y>(
					attributeContext.getPropertyMapping(),
					attributeContext.getOwnerType(),
					member,
					Attribute.PersistentAttributeType.BASIC
			);
		}
		throw new UnsupportedOperationException( "oops, we are missing something: " + attributeContext.getPropertyMapping() );
	}

	public static Attribute.PersistentAttributeType determineSingularAssociationAttributeType(Member member) {
		if ( Field.class.isInstance( member ) ) {
			return ( (Field) member ).getAnnotation( OneToOne.class ) != null
					? Attribute.PersistentAttributeType.ONE_TO_ONE
					: Attribute.PersistentAttributeType.MANY_TO_ONE;
		}
		else {
			return ( (Method) member ).getAnnotation( OneToOne.class ) != null
					? Attribute.PersistentAttributeType.ONE_TO_ONE
					: Attribute.PersistentAttributeType.MANY_TO_ONE;
		}
	}

	private abstract class BaseAttributeMetadata<X,Y> implements AttributeMetadata<X,Y> {
		private final Property propertyMapping;
		private final AbstractManagedType<X> ownerType;
		private final Member member;
		private final Class<Y> javaType;
		private final Attribute.PersistentAttributeType persistentAttributeType;

		@SuppressWarnings({ "unchecked" })
		protected BaseAttributeMetadata(
				Property propertyMapping,
				AbstractManagedType<X> ownerType,
				Member member,
				Attribute.PersistentAttributeType persistentAttributeType) {
			this.propertyMapping = propertyMapping;
			this.ownerType = ownerType;
			this.member = member;
			this.persistentAttributeType = persistentAttributeType;
			final Class declaredType;
			// we can support method or field members here.  Is there really any other valid type?
			if ( Field.class.isInstance( member ) ) {
				declaredType = ( (Field) member ).getType();
			}
			else if ( Method.class.isInstance( member ) ) {
				declaredType = ( (Method) member ).getReturnType();
			}
			else {
				throw new IllegalArgumentException( "Cannot determine java-type from given member [" + member + "]" );
			}
			this.javaType = accountForPrimitiveTypes( declaredType );
		}

		public String getName() {
			return propertyMapping.getName();
		}

		public Member getMember() {
			return member;
		}

		public Class<Y> getJavaType() {
			return javaType;
		}

		public Attribute.PersistentAttributeType getPersistentAttributeType() {
			return persistentAttributeType;
		}

		public AbstractManagedType<X> getOwnerType() {
			return ownerType;
		}

		public boolean isPlural() {
			return propertyMapping.getType().isCollectionType();
		}

		public Property getPropertyMapping() {
			return propertyMapping;
		}
	}

	@SuppressWarnings({ "unchecked" })
	protected <Y> Class<Y> accountForPrimitiveTypes(Class<Y> declaredType) {
//		if ( !declaredType.isPrimitive() ) {
//			return declaredType;
//		}
//
//		if ( Boolean.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Boolean.class;
//		}
//		if ( Character.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Character.class;
//		}
//		if( Byte.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Byte.class;
//		}
//		if ( Short.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Short.class;
//		}
//		if ( Integer.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Integer.class;
//		}
//		if ( Long.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Long.class;
//		}
//		if ( Float.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Float.class;
//		}
//		if ( Double.TYPE.equals( declaredType ) ) {
//			return (Class<Y>) Double.class;
//		}
//
//		throw new IllegalArgumentException( "Unexpected type [" + declaredType + "]" );
		// if the field is defined as int, return int not Integer...
		return declaredType;
	}

	private class SingularAttributeMetadataImpl<X,Y>
			extends BaseAttributeMetadata<X,Y>
			implements SingularAttributeMetadata<X,Y> {
		private final ValueContext valueContext;

		private SingularAttributeMetadataImpl(
				Property propertyMapping,
				AbstractManagedType<X> ownerType,
				Member member,
				Attribute.PersistentAttributeType persistentAttributeType) {
			super( propertyMapping, ownerType, member, persistentAttributeType );
			valueContext = new ValueContext() {
				public Value getValue() {
					return getPropertyMapping().getValue();
				}

				public Class getBindableType() {
					return getAttributeMetadata().getJavaType();
				}

				public ValueClassification getValueClassification() {
					switch ( getPersistentAttributeType() ) {
						case EMBEDDED: {
							return ValueClassification.EMBEDDABLE;
						}
						case BASIC: {
							return ValueClassification.BASIC;
						}
						default: {
							return ValueClassification.ENTITY;
						}
					}
				}

				public AttributeMetadata getAttributeMetadata() {
					return SingularAttributeMetadataImpl.this;
				}
			};
		}

		public ValueContext getValueContext() {
			return valueContext;
		}
	}

	private class PluralAttributeMetadataImpl<X,Y,E>
			extends BaseAttributeMetadata<X,Y>
			implements PluralAttributeMetadata<X,Y,E> {
		private final PluralAttribute.CollectionType attributeCollectionType;
		private final Attribute.PersistentAttributeType elementPersistentAttributeType;
		private final Attribute.PersistentAttributeType keyPersistentAttributeType;
		private final Class elementJavaType;
		private final Class keyJavaType;
		private final ValueContext elementValueContext;
		private final ValueContext keyValueContext;

		private PluralAttributeMetadataImpl(
				Property propertyMapping,
				AbstractManagedType<X> ownerType,
				Member member,
				Attribute.PersistentAttributeType persistentAttributeType,
				Attribute.PersistentAttributeType elementPersistentAttributeType,
				Attribute.PersistentAttributeType keyPersistentAttributeType) {
			super( propertyMapping, ownerType, member, persistentAttributeType );
			this.attributeCollectionType = determineCollectionType( getJavaType() );
			this.elementPersistentAttributeType = elementPersistentAttributeType;
			this.keyPersistentAttributeType = keyPersistentAttributeType;

			ParameterizedType signatureType = getSignatureType( member );
			if ( keyPersistentAttributeType == null ) {
				elementJavaType = signatureType != null ?
						getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] ) :
						Object.class; //FIXME and honor targetEntity?
				keyJavaType = null;
			}
			else {
				keyJavaType = signatureType != null ?
						getClassFromGenericArgument( signatureType.getActualTypeArguments()[0] ) :
						Object.class; //FIXME and honor targetEntity?
				elementJavaType = signatureType != null ?
						getClassFromGenericArgument( signatureType.getActualTypeArguments()[1] ) :
						Object.class; //FIXME and honor targetEntity?
			}

			this.elementValueContext = new ValueContext() {
				public Value getValue() {
					return ( (Collection) getPropertyMapping().getValue() ).getElement();
				}

				public Class getBindableType() {
					return elementJavaType;
				}

				public ValueClassification getValueClassification() {
					switch ( PluralAttributeMetadataImpl.this.elementPersistentAttributeType ) {
						case EMBEDDED: {
							return ValueClassification.EMBEDDABLE;
						}
						case BASIC: {
							return ValueClassification.BASIC;
						}
						default: {
							return ValueClassification.ENTITY;
						}
					}
				}

				public AttributeMetadata getAttributeMetadata() {
					return PluralAttributeMetadataImpl.this;
				}
			};

			// interpret the key, if one
			if ( keyPersistentAttributeType != null ) {
				this.keyValueContext = new ValueContext() {
					public Value getValue() {
						return ( (Map) getPropertyMapping().getValue() ).getIndex();
					}

					public Class getBindableType() {
						return keyJavaType;
					}

					public ValueClassification getValueClassification() {
						switch ( PluralAttributeMetadataImpl.this.keyPersistentAttributeType ) {
							case EMBEDDED: {
								return ValueClassification.EMBEDDABLE;
							}
							case BASIC: {
								return ValueClassification.BASIC;
							}
							default: {
								return ValueClassification.ENTITY;
							}
						}
					}

					public AttributeMetadata getAttributeMetadata() {
						return PluralAttributeMetadataImpl.this;
					}
				};
			}
			else {
				keyValueContext = null;
			}
		}

		private Class<?> getClassFromGenericArgument(java.lang.reflect.Type type) {
			Class<?> javaType;
			Object unsafeElementType = type;
			if ( unsafeElementType instanceof Class ) {
				javaType = (Class) unsafeElementType;
			}
			else if ( unsafeElementType instanceof TypeVariable ) {
				final java.lang.reflect.Type upperBound = ( ( TypeVariable ) unsafeElementType ).getBounds()[0];
				javaType = getClassFromGenericArgument( upperBound );
			}
			else {
				throw new AssertionFailure("Fail to process type argument in a generic declaration. Type: "
						+ type.getClass() );
			}
			return javaType;
		}

		public ValueContext getElementValueContext() {
			return elementValueContext;
		}

		public PluralAttribute.CollectionType getAttributeCollectionType() {
			return attributeCollectionType;
		}

		public ValueContext getMapKeyValueContext() {
			return keyValueContext;
		}
	}

	public static ParameterizedType getSignatureType(Member member) {
		final java.lang.reflect.Type type = Field.class.isInstance( member )
				? ( ( Field ) member ).getGenericType()
				: ( ( Method ) member ).getGenericReturnType();
		//this is a raw type
		if ( type instanceof Class ) return null;
		return (ParameterizedType) type;
	}

	public static PluralAttribute.CollectionType determineCollectionType(Class javaType) {
		if ( java.util.List.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.LIST;
		}
		else if ( java.util.Set.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.SET;
		}
		else if ( java.util.Map.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.MAP;
		}
		else if ( java.util.Collection.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.COLLECTION;
		}
		else {
			throw new IllegalArgumentException( "Expecting collection type [" + javaType.getName() + "]" );
		}
	}

	public static boolean isManyToMany(Member member) {
		return Field.class.isInstance( member )
				? ( (Field) member ).getAnnotation( ManyToMany.class ) != null
				: ( (Method) member ).getAnnotation( ManyToMany.class ) != null;
	}

	private final MemberResolver EMBEDDED_MEMBER_RESOLVER = new MemberResolver() {
		/**
		 * {@inheritDoc}
		 */
		public Member resolveMember(AttributeContext attributeContext) {
			final EmbeddableTypeImpl embeddableType = ( EmbeddableTypeImpl<?> ) attributeContext.getOwnerType();
			final String attributeName = attributeContext.getPropertyMapping().getName();
			return embeddableType.getHibernateType().getTuplizerMapping()
					.getTuplizer( EntityMode.POJO )
					.getGetter( embeddableType.getHibernateType().getPropertyIndex( attributeName ) )
					.getMember();
		}
	};


	private final MemberResolver VIRTUAL_IDENTIFIER_MEMBER_RESOLVER = new MemberResolver() {
		/**
		 * {@inheritDoc}
		 */
		public Member resolveMember(AttributeContext attributeContext) {
			final IdentifiableType identifiableType = (IdentifiableType) attributeContext.getOwnerType();
			final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
			if ( ! entityMetamodel.getIdentifierProperty().isVirtual() ) {
				throw new IllegalArgumentException( "expecting IdClass mapping" );
			}
			org.hibernate.type.Type type = entityMetamodel.getIdentifierProperty().getType();
			if ( ! EmbeddedComponentType.class.isInstance( type ) ) {
				throw new IllegalArgumentException( "expecting IdClass mapping" );
			}

			final EmbeddedComponentType componentType = (EmbeddedComponentType) type;
			final String attributeName = attributeContext.getPropertyMapping().getName();
			return componentType.getTuplizerMapping()
					.getTuplizer( EntityMode.POJO )
					.getGetter( componentType.getPropertyIndex( attributeName ) )
					.getMember();
		}
	};

	/**
	 * A {@link Member} resolver for normal attributes.
	 */
	private final MemberResolver NORMAL_MEMBER_RESOLVER = new MemberResolver() {
		/**
		 * {@inheritDoc}
		 */
		public Member resolveMember(AttributeContext attributeContext) {
			final AbstractManagedType ownerType = attributeContext.getOwnerType();
			final Property property = attributeContext.getPropertyMapping();
			final Type.PersistenceType persistenceType = ownerType.getPersistenceType();
			if ( Type.PersistenceType.EMBEDDABLE == persistenceType ) {
				return EMBEDDED_MEMBER_RESOLVER.resolveMember( attributeContext );
			}
			else if ( Type.PersistenceType.ENTITY == persistenceType
					|| Type.PersistenceType.MAPPED_SUPERCLASS == persistenceType ) {
				final IdentifiableType identifiableType = (IdentifiableType) ownerType;
				final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
				final String propertyName = property.getName();
				final Integer index = entityMetamodel.getPropertyIndexOrNull( propertyName );
				if ( index == null ) {
					// just like in #determineIdentifierJavaMember , this *should* indicate we have an IdClass mapping
					return VIRTUAL_IDENTIFIER_MEMBER_RESOLVER.resolveMember( attributeContext );
				}
				else {
					return entityMetamodel.getTuplizer( EntityMode.POJO )
							.getGetter( index )
							.getMember();
				}
			}
			else {
				throw new IllegalArgumentException( "Unexpected owner type : " + persistenceType );
			}
		}
	};

	private final MemberResolver IDENTIFIER_MEMBER_RESOLVER = new MemberResolver() {
		public Member resolveMember(AttributeContext attributeContext) {
			final IdentifiableType identifiableType = (IdentifiableType) attributeContext.getOwnerType();
			final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
			if ( ! attributeContext.getPropertyMapping().getName()
					.equals( entityMetamodel.getIdentifierProperty().getName() ) ) {
				// this *should* indicate processing part of an IdClass...
				return VIRTUAL_IDENTIFIER_MEMBER_RESOLVER.resolveMember( attributeContext );
			}
			return entityMetamodel.getTuplizer( EntityMode.POJO ).getIdentifierGetter().getMember();
		}
	};

	private final MemberResolver VERSION_MEMBER_RESOLVER = new MemberResolver() {
		public Member resolveMember(AttributeContext attributeContext) {
			final IdentifiableType identifiableType = (IdentifiableType) attributeContext.getOwnerType();
			final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
			final String versionPropertyName = attributeContext.getPropertyMapping().getName();
			if ( ! versionPropertyName.equals( entityMetamodel.getVersionProperty().getName() ) ) {
				// this should never happen, but to be safe...
				throw new IllegalArgumentException( "Given property did not match declared version property" );
			}
			return entityMetamodel.getTuplizer( EntityMode.POJO ).getVersionGetter().getMember();
		}
	};
}
