/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Member;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeImplementor;

/**
 * A "parameter object" for creating a plural attribute
 */
public class PluralAttributeBuilder<D, C, E, K> {
	private final ManagedTypeImplementor<D> declaringType;
	private final SimpleTypeImplementor<E> valueType;

	private Attribute.PersistentAttributeType attributeNature;

	private Property property;
	private Member member;
	private Class<C> collectionClass;

	private SimpleTypeImplementor<K> keyType;


	public PluralAttributeBuilder(
			ManagedTypeImplementor<D> ownerType,
			SimpleTypeImplementor<E> attrType,
			Class<C> collectionClass,
			SimpleTypeImplementor<K> keyType) {
		this.declaringType = ownerType;
		this.valueType = attrType;
		this.collectionClass = collectionClass;
		this.keyType = keyType;
	}

	public ManagedTypeImplementor<D> getDeclaringType() {
		return declaringType;
	}

	public Attribute.PersistentAttributeType getAttributeNature() {
		return attributeNature;
	}

	public SimpleTypeImplementor<K> getKeyType() {
		return keyType;
	}

	public Class<C> getCollectionClass() {
		return collectionClass;
	}

	public SimpleTypeImplementor<E> getValueType() {
		return valueType;
	}

	public Property getProperty() {
		return property;
	}

	public Member getMember() {
		return member;
	}

	public PluralAttributeBuilder<D,C,E,K> member(Member member) {
		this.member = member;
		return this;
	}

	public PluralAttributeBuilder<D,C,E,K> property(Property property) {
		this.property = property;
		return this;
	}

	public PluralAttributeBuilder<D,C,E,K> persistentAttributeType(Attribute.PersistentAttributeType attrType) {
		this.attributeNature = attrType;
		return this;
	}

	@SuppressWarnings( "unchecked" )
	public AbstractPluralAttribute<D,C,E> build() {
		//apply strict spec rules first
		if ( Map.class.equals( collectionClass ) ) {
			final PluralAttributeBuilder<D,Map<K,E>,E,K> builder = (PluralAttributeBuilder<D,Map<K,E>,E,K>) this;
			return (AbstractPluralAttribute<D, C, E>) new MapAttributeImpl<>(
					builder
			);
		}
		else if ( Set.class.equals( collectionClass ) ) {
			final PluralAttributeBuilder<D,Set<E>, E,?> builder = (PluralAttributeBuilder<D, Set<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new SetAttributeImpl<>(
					builder
			);
		}
		else if ( List.class.equals( collectionClass ) ) {
			final PluralAttributeBuilder<D, List<E>, E,?> builder = (PluralAttributeBuilder<D, List<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new ListAttributeImpl<>(
					builder
			);
		}
		else if ( Collection.class.equals( collectionClass ) ) {
			final PluralAttributeBuilder<D, Collection<E>,E,?> builder = (PluralAttributeBuilder<D, Collection<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new CollectionAttributeImpl<>(
					builder
			);
		}

		//apply loose rules
		if ( collectionClass.isArray() ) {
			final PluralAttributeBuilder<D, List<E>, E,?> builder = (PluralAttributeBuilder<D, List<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new ListAttributeImpl<>(
					builder
			);
		}

		if ( Map.class.isAssignableFrom( collectionClass ) ) {
			final PluralAttributeBuilder<D,Map<K,E>,E,K> builder = (PluralAttributeBuilder<D,Map<K,E>,E,K>) this;
			return (AbstractPluralAttribute<D, C, E>) new MapAttributeImpl<>(
					builder
			);
		}
		else if ( Set.class.isAssignableFrom( collectionClass ) ) {
			final PluralAttributeBuilder<D,Set<E>, E,?> builder = (PluralAttributeBuilder<D, Set<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new SetAttributeImpl<>(
					builder
			);
		}
		else if ( List.class.isAssignableFrom( collectionClass ) ) {
			final PluralAttributeBuilder<D, List<E>, E,?> builder = (PluralAttributeBuilder<D, List<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new ListAttributeImpl<>(
					builder
			);
		}
		else if ( Collection.class.isAssignableFrom( collectionClass ) ) {
			final PluralAttributeBuilder<D, Collection<E>,E,?> builder = (PluralAttributeBuilder<D, Collection<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new CollectionAttributeImpl<>(
					builder
			);
		}
		throw new UnsupportedOperationException( "Unkown collection: " + collectionClass );
	}
}
