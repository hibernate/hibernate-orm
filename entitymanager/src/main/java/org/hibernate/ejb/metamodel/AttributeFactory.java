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

import java.util.Iterator;
import java.lang.reflect.Member;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Type;

import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.OneToMany;
import org.hibernate.annotations.common.AssertionFailure;

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
			attribute = buildPluralAttribute( ownerType, property, attrContext );
		}
		else {
			final Type<Y> attrType = getType( attrContext.getElementTypeStatus(), attrContext.getElementValue() );
			attribute = new SingularAttributeImpl<X,Y>(
					property.getName(),
					property.getType().getReturnedClass(),
					ownerType,
					determineJavaMember( property ),
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
		final Type<V> attrType = getType( attrContext.getElementTypeStatus(), attrContext.getElementValue() );
		final Class<Y> collectionClass = (Class<Y>) attrContext.getCollectionClass();
		if ( java.util.Map.class.isAssignableFrom( collectionClass ) ) {
			final Type<K> keyType = getType( attrContext.getKeyTypeStatus(), attrContext.getKeyValue() );
			attribute = PluralAttributeImpl.create( ownerType, attrType, collectionClass, keyType )
					.member( determineJavaMember( property ) )
					.property( property )
					.persistentAttributeType( attrContext.getElementAttributeType() )
					.build();
		}
		else {
			attribute =  PluralAttributeImpl.create( ownerType, attrType, collectionClass, null )
					.member( determineJavaMember( property ) )
					.property( property )
					.persistentAttributeType( attrContext.getElementAttributeType() )
					.build();
		}
		return attribute;
	}

	private <X> Type<X> getType(AttributeContext.TypeStatus elementTypeStatus, Value value) {
		final org.hibernate.type.Type type = value.getType();
		switch ( elementTypeStatus ) {
			case BASIC:
				return buildBasicType( type );
			case EMBEDDABLE:
				return buildEmbeddableType( value, type );
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
	private <X> Type<X> buildEmbeddableType(Value value, org.hibernate.type.Type type) {
		//build embedable type
		final Class<X> clazz = type.getReturnedClass();
		final EmbeddableTypeImpl<X> embeddableType = new EmbeddableTypeImpl<X>( clazz );
		context.registerEmbeddedableType( embeddableType );
		final Component component = (Component) value;
		final Iterator<Property> subProperties = component.getPropertyIterator();
		while ( subProperties.hasNext() ) {
			final Property property = subProperties.next();
			embeddableType.getBuilder().addAttribute( buildAttribute( embeddableType, property ) );
		}
		embeddableType.lock();
		return embeddableType;
	}

	@SuppressWarnings({ "unchecked" })
	public <X, Y> SingularAttributeImpl<X, Y> buildIdAttribute(AbstractManagedType<X> ownerType, Property property) {
		final AttributeContext attrContext = getAttributeContext( property );
		final Type<Y> attrType = getType( attrContext.getElementTypeStatus(), attrContext.getElementValue() );
		final Class<Y> idJavaType = property.getType().getReturnedClass();
		return new SingularAttributeImpl.Identifier(
				property.getName(),
				idJavaType,
				ownerType,
				determineJavaMember( property ),
				attrType,
				attrContext.getElementAttributeType()
		);
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	private Member determineJavaMember(Property property) {
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	public <X, Y> SingularAttributeImpl<X, Y> buildVerisonAttribute(AbstractManagedType<X> ownerType, Property property) {
		final AttributeContext attrContext = getAttributeContext( property );
		final Class<Y> javaType = property.getType().getReturnedClass();
		final Type<Y> attrType = getType( attrContext.getElementTypeStatus(), attrContext.getElementValue() );
		return new SingularAttributeImpl.Version(
				property.getName(),
				javaType,
				ownerType,
				determineJavaMember( property ),
				attrType,
				attrContext.getElementAttributeType()
		);
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

		public Class<?> getCollectionClass() {
			return collectionClass;
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
