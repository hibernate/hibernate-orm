/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.mapping.Property;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public abstract class PluralAttributeImpl<X, C, E>
		extends AbstractAttribute<X,C>
		implements PluralAttribute<X, C, E>, Serializable {

	private final Type<E> elementType;

	private PluralAttributeImpl(Builder<X,C,E,?> builder) {
		super(
				builder.property.getName(),
				builder.collectionClass,
				builder.type,
				builder.member,
				builder.persistentAttributeType
		);
		this.elementType = builder.attributeType;
	}

	public static class Builder<X, C, E, K> {
		private final Type<E> attributeType;
		private final AbstractManagedType<X> type;
		private Member member;
		private PersistentAttributeType persistentAttributeType;
		private Property property;
		private Class<C> collectionClass;
		private Type<K> keyType;


		private Builder(AbstractManagedType<X> ownerType, Type<E> attrType, Class<C> collectionClass, Type<K> keyType) {
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

		@SuppressWarnings( "unchecked" )
		public <K> PluralAttributeImpl<X,C,E> build() {
			//apply strict spec rules first
			if ( Map.class.equals( collectionClass ) ) {
				final Builder<X,Map<K,E>,E,K> builder = (Builder<X,Map<K,E>,E,K>) this;
				return ( PluralAttributeImpl<X, C, E> ) new MapAttributeImpl<X,K,E>(
						builder
				);
			}
			else if ( Set.class.equals( collectionClass ) ) {
				final Builder<X,Set<E>, E,?> builder = (Builder<X, Set<E>, E,?>) this;
				return ( PluralAttributeImpl<X, C, E> ) new SetAttributeImpl<X,E>(
						builder
				);
			}
			else if ( List.class.equals( collectionClass ) ) {
				final Builder<X, List<E>, E,?> builder = (Builder<X, List<E>, E,?>) this;
				return ( PluralAttributeImpl<X, C, E> ) new ListAttributeImpl<X,E>(
						builder
				);
			}
			else if ( Collection.class.equals( collectionClass ) ) {
				final Builder<X, Collection<E>,E,?> builder = (Builder<X, Collection<E>, E,?>) this;
				return ( PluralAttributeImpl<X, C, E> ) new CollectionAttributeImpl<X, E>(
						builder
				);
			}

			//apply loose rules
			if ( Map.class.isAssignableFrom( collectionClass ) ) {
				final Builder<X,Map<K,E>,E,K> builder = (Builder<X,Map<K,E>,E,K>) this;
				return ( PluralAttributeImpl<X, C, E> ) new MapAttributeImpl<X,K,E>(
						builder
				);
			}
			else if ( Set.class.isAssignableFrom( collectionClass ) ) {
				final Builder<X,Set<E>, E,?> builder = (Builder<X, Set<E>, E,?>) this;
				return ( PluralAttributeImpl<X, C, E> ) new SetAttributeImpl<X,E>(
						builder
				);
			}
			else if ( List.class.isAssignableFrom( collectionClass ) ) {
				final Builder<X, List<E>, E,?> builder = (Builder<X, List<E>, E,?>) this;
				return ( PluralAttributeImpl<X, C, E> ) new ListAttributeImpl<X,E>(
						builder
				);
			}
			else if ( Collection.class.isAssignableFrom( collectionClass ) ) {
				final Builder<X, Collection<E>,E,?> builder = (Builder<X, Collection<E>, E,?>) this;
				return ( PluralAttributeImpl<X, C, E> ) new CollectionAttributeImpl<X, E>(
						builder
				);
			}
			throw new UnsupportedOperationException( "Unkown collection: " + collectionClass );
		}
	}

	public static <X,C,E,K> Builder<X,C,E,K> create(
			AbstractManagedType<X> ownerType,
			Type<E> attrType,
			Class<C> collectionClass,
			Type<K> keyType) {
		return new Builder<X,C,E,K>(ownerType, attrType, collectionClass, keyType);
	}

	/**
	 * {@inheritDoc}
	 */
	public Type<E> getElementType() {
		return elementType;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isAssociation() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isCollection() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<E> getBindableJavaType() {
		return elementType.getJavaType();
	}

	static class SetAttributeImpl<X,E> extends PluralAttributeImpl<X,Set<E>,E> implements SetAttribute<X,E> {
		SetAttributeImpl(Builder<X,Set<E>,E,?> xceBuilder) {
			super( xceBuilder );
		}

		/**
		 * {@inheritDoc}
		 */
		public CollectionType getCollectionType() {
			return CollectionType.SET;
		}
	}

	static class CollectionAttributeImpl<X,E> extends PluralAttributeImpl<X,Collection<E>,E> implements CollectionAttribute<X,E> {
		CollectionAttributeImpl(Builder<X, Collection<E>,E,?> xceBuilder) {
			super( xceBuilder );
		}

		/**
		 * {@inheritDoc}
		 */
		public CollectionType getCollectionType() {
			return CollectionType.COLLECTION;
		}
	}

	static class ListAttributeImpl<X,E> extends PluralAttributeImpl<X,List<E>,E> implements ListAttribute<X,E> {
		ListAttributeImpl(Builder<X,List<E>,E,?> xceBuilder) {
			super( xceBuilder );
		}

		/**
		 * {@inheritDoc}
		 */
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

		/**
		 * {@inheritDoc}
		 */
		public CollectionType getCollectionType() {
			return CollectionType.MAP;
		}

		/**
		 * {@inheritDoc}
		 */
		public Class<K> getKeyJavaType() {
			return keyType.getJavaType();
		}

		/**
		 * {@inheritDoc}
		 */
		public Type<K> getKeyType() {
			return keyType;
		}
	}
}
