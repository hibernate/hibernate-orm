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
import java.util.Iterator;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.IdentifiableType;

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

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class AttributeFactory {
	private final MetadataContext context;

	public AttributeFactory(MetadataContext context) {
		this.context = context;
	}

	@SuppressWarnings({ "unchecked" })
	public <X, Y> AttributeImplementor<X, Y> buildAttribute(AbstractManagedType<X> ownerType, Property property) {
		AttributeContext attrContext = getAttributeContext( property );
		final AttributeImplementor<X, Y> attribute;
		if ( attrContext.isCollection() ) {
			attribute = buildPluralAttribute( ownerType, property, attrContext);
		}
		else {
			final Type<Y> attrType = getType( ownerType, attrContext.getElementTypeStatus(), attrContext.getElementValue() );
			attribute = new SingularAttributeImpl<X,Y>(
					property.getName(),
					property.getType().getReturnedClass(),
					ownerType,
					determineStandardJavaMember( ownerType, property ),
					false,
					false,
					property.isOptional(),
					attrType,
					attrContext.getElementAttributeType()
			);
		}
		return attribute;
	}

	@SuppressWarnings( "unchecked" )
	private <X, Y, V, K> AttributeImplementor<X, Y> buildPluralAttribute(AbstractManagedType<X> ownerType, Property property, AttributeContext attrContext) {
		AttributeImplementor<X, Y> attribute;
		final Type<V> attrType = getType( ownerType, attrContext.getElementTypeStatus(), attrContext.getElementValue() );
		final Member member = determineStandardJavaMember( ownerType, property );
		final Class<Y> collectionClass = (Class<Y>) ( member instanceof Field
				? ( ( Field ) member ).getType()
				: ( ( Method ) member ).getReturnType() );
		if ( java.util.Map.class.isAssignableFrom( collectionClass ) ) {
			final Type<K> keyType = getType( ownerType, attrContext.getKeyTypeStatus(), attrContext.getKeyValue() );
			attribute = PluralAttributeImpl.create( ownerType, attrType, collectionClass, keyType )
					.member( member )
					.property( property )
					.persistentAttributeType( attrContext.getElementAttributeType() )
					.build();
		}
		else {
			attribute =  PluralAttributeImpl.create( ownerType, attrType, collectionClass, null )
					.member( member )
					.property( property )
					.persistentAttributeType( attrContext.getElementAttributeType() )
					.build();
		}
		return attribute;
	}

	private <X> Type<X> getType(AbstractManagedType owner, AttributeContext.TypeStatus elementTypeStatus, Value value) {
		final org.hibernate.type.Type type = value.getType();
		switch ( elementTypeStatus ) {
			case BASIC:
				return buildBasicType( type );
			case EMBEDDABLE:
				return buildEmbeddableType( owner, value, type );
			case ENTITY:
				return buildEntityType( type );
			default:
				throw new AssertionFailure( "Unknown AttributeContext.TypeStatus: " + elementTypeStatus );

		}
	}

	@SuppressWarnings( "unchecked" )
	private <X> Type<X> buildBasicType(org.hibernate.type.Type type) {
		final Class<X> clazz = type.getReturnedClass();
		return new BasicTypeImpl<X>( clazz, Type.PersistenceType.BASIC );
	}

	@SuppressWarnings( "unchecked" )
	private <X> Type<X> buildEntityType(org.hibernate.type.Type type) {
		String entityName = ( (org.hibernate.type.EntityType) type ).getAssociatedEntityName();
		return ( Type<X> ) context.locateEntityType( entityName );
	}

	@SuppressWarnings( "unchecked" )
	private <X> Type<X> buildEmbeddableType(AbstractManagedType owner, Value value, org.hibernate.type.Type type) {
		//build embedable type
		final Class<X> clazz = type.getReturnedClass();
		final EmbeddableTypeImpl<X> embeddableType = new EmbeddableTypeImpl<X>( clazz, owner, (ComponentType) type );
		context.registerEmbeddedableType( embeddableType );
		final Component component = (Component) value;
		final Iterator<Property> subProperties = component.getPropertyIterator();
		while ( subProperties.hasNext() ) {
			final Property property = subProperties.next();
			embeddableType.getBuilder().addAttribute( buildAttribute( embeddableType, property) );
		}
		embeddableType.lock();
		return embeddableType;
	}

	@SuppressWarnings({ "unchecked" })
	public <X, Y> SingularAttributeImpl<X, Y> buildIdAttribute(AbstractIdentifiableType<X> ownerType, Property property) {
		final AttributeContext attrContext = getAttributeContext( property );
		final Type<Y> attrType = getType( ownerType, attrContext.getElementTypeStatus(), attrContext.getElementValue() );
		final Class<Y> idJavaType = property.getType().getReturnedClass();
		return new SingularAttributeImpl.Identifier(
				property.getName(),
				idJavaType,
				ownerType,
				determineIdentifierJavaMember( ownerType, property ),
				attrType,
				attrContext.getElementAttributeType()
		);
	}

	private Member determineIdentifierJavaMember(IdentifiableType ownerType, Property property) {
// see below
//		final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( property );
		final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( ownerType );
		if ( ! property.getName().equals( entityMetamodel.getIdentifierProperty().getName() ) ) {
			// this *should* indicate processing part of an IdClass...
			return determineVirtualIdentifierJavaMember( entityMetamodel, property );
		}
		return entityMetamodel.getTuplizer( EntityMode.POJO ).getIdentifierGetter().getMember();
	}

	private Member determineVirtualIdentifierJavaMember(EntityMetamodel entityMetamodel, Property property) {
		if ( ! entityMetamodel.getIdentifierProperty().isVirtual() ) {
			throw new IllegalArgumentException( "expecting IdClass mapping" );
		}
		org.hibernate.type.Type type = entityMetamodel.getIdentifierProperty().getType();
		if ( ! EmbeddedComponentType.class.isInstance( type ) ) {
			throw new IllegalArgumentException( "expecting IdClass mapping" );
		}
		final EmbeddedComponentType componentType = (EmbeddedComponentType) type;
		return componentType.getTuplizerMapping()
				.getTuplizer( EntityMode.POJO )
				.getGetter( componentType.getPropertyIndex( property.getName() ) )
				.getMember();
	}

// getting the owning PersistentClass from the Property is broken in certain cases with annotations...
//	private EntityMetamodel getDeclarerEntityMetamodel(Property property) {
//		return context.getSessionFactory()
//				.getEntityPersister( property.getPersistentClass().getEntityName() )
//				.getEntityMetamodel();
//	}
// so we use the owner's java class to lookup the persister/entitymetamodel
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


// getting the owning PersistentClass from the Property is broken in certain cases with annotations...
//	private Member determineStandardJavaMember(Property property) {
//		final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( property );
//
//		final String propertyName = property.getName();
//		final int index = entityMetamodel.getPropertyIndex( propertyName );
//		return entityMetamodel.getTuplizer( EntityMode.POJO ).getGetter( index ).getMember();
//	}
// so we use the owner's java class to lookup the persister/entitymetamodel
	private Member determineStandardJavaMember(AbstractManagedType<?> ownerType, Property property) {
	final Type.PersistenceType persistenceType = ownerType.getPersistenceType();
	if ( Type.PersistenceType.EMBEDDABLE == persistenceType ) {
			EmbeddableTypeImpl embeddableType = ( EmbeddableTypeImpl<?> ) ownerType;
			return embeddableType.getHibernateType().getTuplizerMapping()
					.getTuplizer( EntityMode.POJO )
					.getGetter( embeddableType.getHibernateType().getPropertyIndex( property.getName() ) )
					.getMember();
		}
		else if ( Type.PersistenceType.ENTITY == persistenceType
			|| Type.PersistenceType.MAPPED_SUPERCLASS == persistenceType ) {
			return determineStandardJavaMemberOutOfIdentifiableType( (IdentifiableType<?>) ownerType, property );
		}
		else {
			throw new IllegalArgumentException( "Unexpected owner type : " + persistenceType );
		}
	}

	private Member determineStandardJavaMemberOutOfIdentifiableType(IdentifiableType<?> ownerType, Property property) {
		final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( ownerType );
		final String propertyName = property.getName();
		final Integer index = entityMetamodel.getPropertyIndexOrNull( propertyName );
		if ( index == null ) {
			// just like in #determineIdentifierJavaMember , this *should* indicate we have an IdClass mapping
			return determineVirtualIdentifierJavaMember( entityMetamodel, property );
		}
		else {
			return entityMetamodel.getTuplizer( EntityMode.POJO ).getGetter( index ).getMember();
		}
	}

	@SuppressWarnings({ "unchecked" })
	public <X, Y> SingularAttributeImpl<X, Y> buildVersionAttribute(AbstractIdentifiableType<X> ownerType, Property property) {
		final AttributeContext attrContext = getAttributeContext( property );
		final Class<Y> javaType = property.getType().getReturnedClass();
		final Type<Y> attrType = getType( ownerType, attrContext.getElementTypeStatus(), attrContext.getElementValue() );
		return new SingularAttributeImpl.Version(
				property.getName(),
				javaType,
				ownerType,
				determineVersionJavaMember( ownerType, property ),
				attrType,
				attrContext.getElementAttributeType()
		);
	}

	private Member determineVersionJavaMember(IdentifiableType ownerType, Property property) {
		final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( ownerType );
		if ( ! property.getName().equals( entityMetamodel.getVersionProperty().getName() ) ) {
			// this should never happen, but to be safe...
			throw new IllegalArgumentException( "Given property did not match declared version property" );
		}
		return entityMetamodel.getTuplizer( EntityMode.POJO ).getVersionGetter().getMember();
	}

	private static class AttributeContext {
		private final Value elementValue;
		private final TypeStatus typeStatus;
		private final Class<?> collectionClass;
		private final Attribute.PersistentAttributeType attrType;
		private final Value keyValue;
		private final TypeStatus keyTypeStatus;

		enum TypeStatus {
			EMBEDDABLE,
			ENTITY,
			BASIC
		}

		private AttributeContext(
				Value elementValue,
				TypeStatus elementTypeStatus,
				Attribute.PersistentAttributeType elementPAT,
				Class<?> collectionClass,
				Value keyValue,
				TypeStatus keyTypeStatus) {
			this.elementValue = elementValue;
			this.typeStatus = elementTypeStatus;
			this.collectionClass = collectionClass;
			this.attrType = elementPAT;
			this.keyValue = keyValue;
			this.keyTypeStatus = keyTypeStatus;
		}

		public Value getElementValue() {
			return elementValue;
		}

		public TypeStatus getElementTypeStatus() {
			return typeStatus;
		}

		public boolean isCollection() {
			return collectionClass != null;
		}

		public Attribute.PersistentAttributeType getElementAttributeType() {
			return attrType;
		}

		public Value getKeyValue() {
			return keyValue;
		}

		public TypeStatus getKeyTypeStatus() {
			return keyTypeStatus;
		}
	}

	private static AttributeContext getAttributeContext(Property property) {
		// FIXME the logical level for *To* is different from the Hibernate physical model.
		//		ie a @ManyToOne @AssocTable is a many-to-many for hibernate
		//		and a @OneToMany @AssocTable is a many-to-many for hibernate
		// FIXME so basically Attribute.PersistentAttributeType is crap at the moment
		final Value value = property.getValue();
		final org.hibernate.type.Type type = value.getType();
		if ( type.isAnyType() ) {
			throw new UnsupportedOperationException( "any not supported yet" );
		}
		else if ( type.isAssociationType() ) {
			//collection or entity
			if ( type.isCollectionType() ) {
				//do collection
				if ( value instanceof Collection ) {
					final Collection collValue = ( Collection ) value;
					final Value elementValue = collValue.getElement();
					final org.hibernate.type.Type elementType = elementValue.getType();
					final AttributeContext.TypeStatus elementTypeStatus;
					final Attribute.PersistentAttributeType elementPAT;
					final Class<?> collectionClass = collValue.getCollectionType().getReturnedClass();

					final Value keyValue;
					final org.hibernate.type.Type keyType;
					final AttributeContext.TypeStatus keyTypeStatus;
					if ( value instanceof Map ) {
						keyValue = ( ( Map ) value ).getIndex();
						keyType = keyValue.getType();
						if ( keyValue instanceof Component ) {
							keyTypeStatus = AttributeContext.TypeStatus.EMBEDDABLE;
						}
						else if ( keyType.isAnyType() ) {
							throw new UnsupportedOperationException( "collection of any not supported yet" );
						}
						else if ( keyType.isAssociationType() ) {
							keyTypeStatus = AttributeContext.TypeStatus.ENTITY;
						}
						else {
							keyTypeStatus = AttributeContext.TypeStatus.BASIC;
						}
					}
					else {
						keyValue = null;
						keyTypeStatus = null;
					}

					if ( elementValue instanceof Component ) {
						//collection of components
						elementTypeStatus = AttributeContext.TypeStatus.EMBEDDABLE;
						elementPAT = Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
					}
					else if ( elementType.isAnyType() ) {
						throw new UnsupportedOperationException( "collection of any not supported yet" );
					}
					else if ( elementType.isAssociationType() ) {
						//collection of entity
						elementTypeStatus = AttributeContext.TypeStatus.ENTITY;
						elementPAT = Attribute.PersistentAttributeType.MANY_TO_MANY;
					}
					else {
						//collection of basic type
						elementTypeStatus = AttributeContext.TypeStatus.BASIC;
						elementPAT = Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
					}
					return new AttributeContext(
							elementValue,
							elementTypeStatus,
							elementPAT,
							collectionClass,
							keyValue,
							keyTypeStatus
					);
				}
				else if ( value instanceof OneToMany ) {
					//one to many with FK => entity
					return new AttributeContext(
							value,
							AttributeContext.TypeStatus.ENTITY,
							Attribute.PersistentAttributeType.ONE_TO_MANY,
							null, null, null
					);
				}

			}
			else {
				//ToOne association
				return new AttributeContext(
						value,
						AttributeContext.TypeStatus.ENTITY,
						Attribute.PersistentAttributeType.MANY_TO_MANY,
						//FIXME how to differentiate the logical many to one from the one to one (not physical level)
						null,
						null,
						null
				);
			}
		}
		else if ( property.isComposite() ) {
			//embeddable
			return new AttributeContext(
					value,
					AttributeContext.TypeStatus.EMBEDDABLE,
					Attribute.PersistentAttributeType.EMBEDDED,
					null, null, null
			);

		}
		else {
			//basic type
			return new AttributeContext(
					value,
					AttributeContext.TypeStatus.BASIC,
					Attribute.PersistentAttributeType.BASIC,
					null, null, null
			);
		}
		throw new UnsupportedOperationException( "oops, we are missing something: " + property.toString() );
	}


}
