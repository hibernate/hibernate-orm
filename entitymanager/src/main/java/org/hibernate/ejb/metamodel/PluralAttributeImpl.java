package org.hibernate.ejb.metamodel;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.io.Serializable;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;

import org.hibernate.mapping.Property;

/**
 * @author Emmanuel Bernard
 */
public abstract class PluralAttributeImpl<X, C, E> implements PluralAttribute<X, C, E>, Serializable {

	private final ManagedType<X> ownerType;
	private final Type<E> elementType;
	//FIXME member is not serializable
	private final Member member;
	private final String name;
	private final PersistentAttributeType persistentAttributeType;
	private final Class<C> collectionClass;

	private PluralAttributeImpl(Builder<X,C,E,?> builder) {
		this.ownerType = builder.type;
		this.elementType = builder.attributeType;
		this.collectionClass = builder.collectionClass;
		this.member = builder.member;
		this.name = builder.property.getName();
		this.persistentAttributeType = builder.persistentAttributeType;
	}

	public static class Builder<X, C, E, K> {
		private final Type<E> attributeType;
		private final ManagedType<X> type;
		private Member member;
		private PersistentAttributeType persistentAttributeType;
		private Property property;
		private Class<C> collectionClass;
		private Type<K> keyType;


		private Builder(ManagedType<X> ownerType, Type<E> attrType, Class<C> collectionClass, Type<K> keyType) {
			this.type = ownerType;
			this.attributeType = attrType;
			this.collectionClass = collectionClass;
			this.keyType = keyType;
		}

		public Builder<X,C,E,K> member(Member member) {
			this.member = member;
			return this;
		}

		public Builder<X,C,E,K> property(Property property) {
			this.property = property;
			return this;
		}

		public Builder<X,C,E,K> persistentAttributeType(PersistentAttributeType attrType) {
			this.persistentAttributeType = attrType;
			return this;
		}

		public <K> PluralAttributeImpl<X,C,E> build() {
			if ( Map.class.isAssignableFrom( collectionClass ) ) {
				@SuppressWarnings( "unchecked" )
				final Builder<X,Map<K,E>,E,K> builder = (Builder<X,Map<K,E>,E,K>) this;
				@SuppressWarnings( "unchecked" )
				final PluralAttributeImpl<X, C, E> result = ( PluralAttributeImpl<X, C, E> ) new MapAttributeImpl<X,K,E>(
						builder
				);
				return result;
			}
			else if ( Set.class.isAssignableFrom( collectionClass ) ) {
				@SuppressWarnings( "unchecked" )
				final Builder<X,Set<E>, E,?> builder = (Builder<X, Set<E>, E,?>) this;
				@SuppressWarnings( "unchecked" )
				final PluralAttributeImpl<X, C, E> result = ( PluralAttributeImpl<X, C, E> ) new SetAttributeImpl<X,E>(
						builder
				);
				return result;
			}
			else if ( List.class.isAssignableFrom( collectionClass ) ) {
				@SuppressWarnings( "unchecked" )
				final Builder<X, List<E>, E,?> builder = (Builder<X, List<E>, E,?>) this;
				@SuppressWarnings( "unchecked" )
				final PluralAttributeImpl<X, C, E> result = ( PluralAttributeImpl<X, C, E> ) new ListAttributeImpl<X,E>(
						builder
				);
				return result;
			}
			else if ( Collection.class.isAssignableFrom( collectionClass ) ) {
				@SuppressWarnings( "unchecked" )
				final Builder<X, Collection<E>,E,?> builder = (Builder<X, Collection<E>, E,?>) this;
				@SuppressWarnings( "unchecked" )
				final PluralAttributeImpl<X, C, E> result = ( PluralAttributeImpl<X, C, E> ) new CollectionAttributeImpl<X, E>(
						builder
				);
				return result;
			}
			throw new UnsupportedOperationException( "Unkown collection: " + collectionClass );
		}
	}

	public static <X,C,E,K> Builder<X,C,E,K> create(
			ManagedType<X> ownerType,
			Type<E> attrType,
			Class<C> collectionClass,
			Type<K> keyType) {
		return new Builder<X,C,E,K>(ownerType, attrType, collectionClass, keyType);
	}

	public String getName() {
		return name;
	}

	public PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	public ManagedType<X> getDeclaringType() {
		return ownerType;
	}

	public Class<C> getJavaType() {
		return collectionClass;
	}

	public abstract CollectionType getCollectionType();

	public Type<E> getElementType() {
		return elementType;
	}


	public Member getJavaMember() {
		return member;
	}

	public boolean isAssociation() {
		return true;
	}

	public boolean isCollection() {
		return true;
	}

	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	public Class<E> getBindableJavaType() {
		return elementType.getJavaType();
	}

	static class SetAttributeImpl<X,E> extends PluralAttributeImpl<X,Set<E>,E> implements SetAttribute<X,E> {
		SetAttributeImpl(Builder<X,Set<E>,E,?> xceBuilder) {
			super( xceBuilder );
		}

		public CollectionType getCollectionType() {
			return CollectionType.SET;
		}
	}

	static class CollectionAttributeImpl<X,E> extends PluralAttributeImpl<X,Collection<E>,E> implements CollectionAttribute<X,E> {
		CollectionAttributeImpl(Builder<X, Collection<E>,E,?> xceBuilder) {
			super( xceBuilder );
		}

		public CollectionType getCollectionType() {
			return CollectionType.COLLECTION;
		}
	}

	static class ListAttributeImpl<X,E> extends PluralAttributeImpl<X,List<E>,E> implements ListAttribute<X,E> {
		ListAttributeImpl(Builder<X,List<E>,E,?> xceBuilder) {
			super( xceBuilder );
		}

		public CollectionType getCollectionType() {
			return CollectionType.LIST;
		}
	}

	static class MapAttributeImpl<X,K,V> extends PluralAttributeImpl<X,Map<K,V>,V> implements MapAttribute<X,K,V> {
		private final Type<K> keyType;

		MapAttributeImpl(Builder<X,Map<K,V>,V,K> xceBuilder) {
			super( xceBuilder );
			this.keyType = xceBuilder.keyType;
		}

		public CollectionType getCollectionType() {
			return CollectionType.MAP;
		}

		public Class<K> getKeyJavaType() {
			return keyType.getJavaType();
		}

		public Type<K> getKeyType() {
			return keyType;
		}
	}
}
