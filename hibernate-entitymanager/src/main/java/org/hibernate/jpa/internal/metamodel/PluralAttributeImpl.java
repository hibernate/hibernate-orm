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
package org.hibernate.jpa.internal.metamodel;

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

import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public abstract class PluralAttributeImpl<X, C, E>
		extends AbstractAttribute<X,C>
		implements PluralAttribute<X, C, E>, Serializable {

	private final Type<E> elementType;

	@SuppressWarnings("unchecked")
	private PluralAttributeImpl(Builder builder) {
		super(
				builder.attributeBinding.getAttribute().getName(),
				builder.collectionClass,
				builder.owner,
				builder.member,
				builder.persistentAttributeType
		);
		this.elementType = builder.elementType;
	}

	public static class Builder {
		private final Class collectionClass;
		private AbstractManagedType owner;
		private PluralAttributeBinding attributeBinding;
		private Member member;
		private Type keyType;
		private Type elementType;
		private PersistentAttributeType persistentAttributeType;

		public Builder(Class collectionClass) {
			this.collectionClass = collectionClass;
		}

		public Builder owner(AbstractManagedType owner) {
			this.owner = owner;
			return this;
		}

		public Builder member(Member member) {
			this.member = member;
			return this;
		}

		public Builder binding(PluralAttributeBinding attributeBinding) {
			this.attributeBinding = attributeBinding;
			return this;
		}

		public Builder elementType(Type elementType) {
			this.elementType = elementType;
			return this;
		}

		public Builder keyType(Type keyType) {
			this.keyType = keyType;
			return this;
		}

		public Builder persistentAttributeType(PersistentAttributeType attrType) {
			this.persistentAttributeType = attrType;
			return this;
		}

		@SuppressWarnings( "unchecked" )
		public <X,C,E,K> PluralAttributeImpl<X,C,E> build() {
			//apply strict spec rules first
			if ( Map.class.equals( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new MapAttributeImpl<X,K,E>( this );
			}
			else if ( Set.class.equals( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new SetAttributeImpl<X,E>( this );
			}
			else if ( List.class.equals( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new ListAttributeImpl<X,E>( this );
			}
			else if ( Collection.class.equals( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new CollectionAttributeImpl<X, E>( this );
			}

			//apply loose rules
			if ( Map.class.isAssignableFrom( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new MapAttributeImpl<X,K,E>( this );
			}
			else if ( Set.class.isAssignableFrom( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new SetAttributeImpl<X,E>( this );
			}
			else if ( List.class.isAssignableFrom( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new ListAttributeImpl<X,E>( this );
			}
			else if ( Collection.class.isAssignableFrom( collectionClass ) ) {
				return ( PluralAttributeImpl<X, C, E> ) new CollectionAttributeImpl<X, E>( this );
			}
			throw new UnsupportedOperationException( "Unknown collection: " + collectionClass );
		}
	}

	public static Builder builder(Class collectionClass) {
		return new Builder( collectionClass );
	}

	@Override
	public Type<E> getElementType() {
		return elementType;
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	@Override
	public Class<E> getBindableJavaType() {
		return elementType.getJavaType();
	}

	static class SetAttributeImpl<X,E> extends PluralAttributeImpl<X,Set<E>,E> implements SetAttribute<X,E> {
		SetAttributeImpl(Builder xceBuilder) {
			super( xceBuilder );
		}

		@Override
		public CollectionType getCollectionType() {
			return CollectionType.SET;
		}
	}

	static class CollectionAttributeImpl<X,E> extends PluralAttributeImpl<X,Collection<E>,E> implements CollectionAttribute<X,E> {
		CollectionAttributeImpl(Builder xceBuilder) {
			super( xceBuilder );
		}

		@Override
		public CollectionType getCollectionType() {
			return CollectionType.COLLECTION;
		}
	}

	static class ListAttributeImpl<X,E> extends PluralAttributeImpl<X,List<E>,E> implements ListAttribute<X,E> {
		ListAttributeImpl(Builder xceBuilder) {
			super( xceBuilder );
		}

		@Override
		public CollectionType getCollectionType() {
			return CollectionType.LIST;
		}
	}

	static class MapAttributeImpl<X,K,V> extends PluralAttributeImpl<X,Map<K,V>,V> implements MapAttribute<X,K,V> {
		private final Type<K> keyType;

		@SuppressWarnings("unchecked")
		MapAttributeImpl(Builder xceBuilder) {
			super( xceBuilder );
			this.keyType = xceBuilder.keyType;
		}

		@Override
		public CollectionType getCollectionType() {
			return CollectionType.MAP;
		}

		@Override
		public Class<K> getKeyJavaType() {
			return keyType.getJavaType();
		}

		@Override
		public Type<K> getKeyType() {
			return keyType;
		}
	}
}
