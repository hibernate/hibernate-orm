package org.hibernate.ejb.metamodel;

import java.util.Iterator;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

/**
 * @author Emmanuel Bernard
 */
class MetamodelFactory {

	static<X, Y, V, K> Attribute<X, Y> getAttribute(ManagedType<X> ownerType, Property property, MetadataContext metadataContext) {
		AttributeContext attrContext = getAttributeContext( property );
		final Attribute<X, Y> attribute;
		if ( attrContext.isCollection() ) {
			final Type<V> attrType = getType( attrContext.getElementTypeStatus(), attrContext.getElementValue(), metadataContext );
			@SuppressWarnings( "unchecked" )
			final Class<Y> collectionClass = (Class<Y>) attrContext.getCollectionClass();
			if ( java.util.Map.class.isAssignableFrom( collectionClass ) ) {
				final Type<K> keyType = getType( attrContext.getKeyTypeStatus(), attrContext.getKeyValue(), metadataContext );
				attribute = PluralAttributeImpl.create( ownerType, attrType, collectionClass, keyType )
						// .member(  ); //TODO member
						.property( property )
						.persistentAttributeType( attrContext.getElementAttributeType() )
						.build();
			}
			else {
				attribute =  PluralAttributeImpl.create( ownerType, attrType, collectionClass, null )
						// .member(  ); //TODO member
						.property( property )
						.persistentAttributeType( attrContext.getElementAttributeType() )
						.build();
			}
		}
		else {
			final Type<Y> attrType = getType( attrContext.getElementTypeStatus(), attrContext.getElementValue(), metadataContext );
			attribute = SingularAttributeImpl.create( ownerType, attrType )
					// .member(  ); //TODO member
					.property( property )
					.persistentAttributeType( attrContext.getElementAttributeType() )
					.build();
		}
		return attribute;
	}

	private static <X> Type<X> getType(AttributeContext.TypeStatus elementTypeStatus, Value value, MetadataContext metadataContext) {
		final org.hibernate.type.Type type = value.getType();
		switch ( elementTypeStatus ) {
			case BASIC:
				return buildBasicType( type );
			case EMBEDDABLE:
				return buildEmbeddableType( value, type, metadataContext );
			case ENTITY:
				return buildEntityType( type, metadataContext );
			default:
				throw new AssertionFailure( "Unknown AttributeContext.TypeStatus: " + elementTypeStatus );

		}
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

		AttributeContext(Value elementValue,
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


	//FIXME the logical level for *To* is different from the Hibernate physical model.
	//ie a @ManyToOne @AssocTable is a many-to-many for hibernate
	//and a @OneToMany @AssocTable is a many-to-many for hibernate
	//FIXME so basically Attribute.PersistentAttributeType is crap at the moment
	private static AttributeContext getAttributeContext(Property property) {
		final Value value = property.getValue();
		org.hibernate.type.Type type = value.getType();
		if ( type.isAnyType() ) {
			throw new UnsupportedOperationException( "any not supported yet" );
		}
		else if ( type.isAssociationType() ) {
			//collection or entity
			if ( type.isCollectionType() ) {
				//do collection
				if ( value instanceof Collection ) {
					final Collection collValue = (Collection) value;
					final Value elementValue = collValue.getElement();
					final org.hibernate.type.Type elementType = elementValue.getType();
					final AttributeContext.TypeStatus elementTypeStatus;
					final Attribute.PersistentAttributeType elementPAT;
					final Class<?> collectionClass = collValue.getCollectionType().getReturnedClass();


					final Value keyValue;
					final org.hibernate.type.Type keyType;
					final AttributeContext.TypeStatus keyTypeStatus;
					if ( value instanceof Map ) {
						keyValue = ( (Map) value).getIndex();
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
							null, null, null );
				}

			}
			else {
				//ToOne association
				return new AttributeContext(
						value,
						AttributeContext.TypeStatus.ENTITY,
						Attribute.PersistentAttributeType.MANY_TO_MANY, //FIXME how to differentiate the logical many to one from the one to one (not physical level)
						null, null, null);
			}
		}
		else if ( property.isComposite() ) {
			//embeddable
			return new AttributeContext(
					value,
					AttributeContext.TypeStatus.EMBEDDABLE,
					Attribute.PersistentAttributeType.EMBEDDED,
					null, null, null);

		}
		else {
			//basic type
			return new AttributeContext(
					value,
					AttributeContext.TypeStatus.BASIC,
					Attribute.PersistentAttributeType.BASIC,
					null, null, null);
		}
		throw new UnsupportedOperationException("oops, we are missing something: " + property.toString() );
	}



	static <X> Type<X> getType(Property property, MetadataContext context) {
		final Value value = property.getValue();
		org.hibernate.type.Type type = value.getType();
		if ( type.isAnyType() ) {
			throw new UnsupportedOperationException( "any not supported yet" );
		}
		else if ( type.isAssociationType() ) {
			//collection or entity
			if ( type.isCollectionType() ) {
				//do collection
				if ( value instanceof Collection ) {
					Collection collValue = (Collection) value;
					collValue.getCollectionType();
					Value elementValue = collValue.getElement();
					final org.hibernate.type.Type elementType = elementValue.getType();
					if ( elementValue instanceof Component ) {
						//colelction of components
						return buildEmbeddableType( elementValue, elementType, context );
					}
					else if ( elementType.isAnyType() ) {
						throw new UnsupportedOperationException( "collection of any not supported yet" );
					}
					else if ( elementType.isAssociationType() ) {
						//collection of entity
						return buildEntityType( elementType, context);
					}
					else {
						//collection of basic type
						buildBasicType( elementType );
					}
				}
				else if ( value instanceof OneToMany ) {
					//one to many with FK => entity
					return buildEntityType( value.getType(), context );
				}
				
			}
			else {
				//ToOne association
				return buildEntityType( type, context );
			}
		}
		else if ( property.isComposite() ) {
			//embeddable
			return buildEmbeddableType( value, type, context );

		}
		else {
			//basic type
			return buildBasicType( type );
		}
		throw new UnsupportedOperationException("oops, we are missing something: " + property.toString() );
	}

	private static <X> Type<X> buildBasicType(org.hibernate.type.Type type) {
		@SuppressWarnings( "unchecked" )
		final Class<X> clazz = type.getReturnedClass();
		return new BasicTypeImpl<X>( clazz, Type.PersistenceType.BASIC );
	}

	private static <X> Type<X> buildEntityType(org.hibernate.type.Type type, MetadataContext context) {
		@SuppressWarnings( "unchecked" )
		final Class<X> clazz = type.getReturnedClass();
		final EntityTypeDelegator<X> entityTypeDelegator = new EntityTypeDelegator<X>();
		context.addDelegator( entityTypeDelegator, clazz );
		return entityTypeDelegator;
	}

	private static <X> Type<X> buildEmbeddableType(Value value, org.hibernate.type.Type type, MetadataContext context) {
		//build embedable type
		@SuppressWarnings( "unchecked" )
		final Class<X> clazz = type.getReturnedClass();
		Component component = (Component) value;
		@SuppressWarnings( "unchecked")
		final Iterator<Property> subProperties = component.getPropertyIterator();
		final EmbeddableType<X> embeddableType = new EmbeddableTypeImpl<X>( clazz, subProperties, context );
		context.addEmbeddableType( clazz, embeddableType );
		return embeddableType;
	}
}
