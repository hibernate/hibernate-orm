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

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A "parameter object" for creating a plural attribute
 */
public class PluralAttributeBuilder<D, C, E, K> {
	private final ManagedDomainType<D> declaringType;
	private final SimpleDomainType<E> valueType;

	private final NodeBuilder nodeBuilder;

	private SimpleDomainType<K> listIndexOrMapKeyType;

	private AttributeClassification attributeClassification;
	private CollectionClassification collectionClassification;
	private JavaTypeDescriptor<C> collectionJavaTypeDescriptor;

	private Property property;
	private Member member;

	public PluralAttributeBuilder(
			ManagedDomainType<D> ownerType,
			SimpleDomainType<E> elementType,
			JavaTypeDescriptor<C> collectionJavaTypeDescriptor,
			SimpleDomainType<K> listIndexOrMapKeyType,
			NodeBuilder nodeBuilder) {
		this.declaringType = ownerType;
		this.valueType = elementType;
		this.collectionJavaTypeDescriptor = collectionJavaTypeDescriptor;
		this.listIndexOrMapKeyType = listIndexOrMapKeyType;
		this.nodeBuilder = nodeBuilder;
	}

	public NodeBuilder getNodeBuilder() {
		return nodeBuilder;
	}

	public ManagedDomainType<D> getDeclaringType() {
		return declaringType;
	}

	public AttributeClassification getAttributeClassification() {
		return attributeClassification;
	}

	public CollectionClassification getCollectionClassification() {
		return collectionClassification;
	}

	public SimpleDomainType<K> getListIndexOrMapKeyType() {
		return listIndexOrMapKeyType;
	}

	public JavaTypeDescriptor<C> getCollectionJavaTypeDescriptor() {
		return collectionJavaTypeDescriptor;
	}

	public SimpleDomainType<E> getValueType() {
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

	public PluralAttributeBuilder<D,C,E,K> persistentAttributeClassification(AttributeClassification classification) {
		this.attributeClassification = classification;
		return this;
	}

	@SuppressWarnings( "unchecked" )
	public AbstractPluralAttribute<D,C,E> build() {
		//apply strict spec rules first
		if ( Map.class.equals( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D,Map<K,E>,E,K> builder = (PluralAttributeBuilder<D,Map<K,E>,E,K>) this;
			return (AbstractPluralAttribute<D, C, E>) new MapAttributeImpl<>(
					builder
			);
		}
		else if ( Set.class.equals( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D,Set<E>, E,?> builder = (PluralAttributeBuilder<D, Set<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new SetAttributeImpl<>(
					builder
			);
		}
		else if ( List.class.equals( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D, List<E>, E,?> builder = (PluralAttributeBuilder<D, List<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new ListAttributeImpl<>(
					builder
			);
		}
		else if ( Collection.class.equals( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D, Collection<E>,E,?> builder = (PluralAttributeBuilder<D, Collection<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new BagAttributeImpl<>(
					builder
			);
		}

		//apply loose rules
		if ( collectionJavaTypeDescriptor.getJavaType().isArray() ) {
			final PluralAttributeBuilder<D, List<E>, E,?> builder = (PluralAttributeBuilder<D, List<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new ListAttributeImpl<>(
					builder
			);
		}

		if ( Map.class.isAssignableFrom( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D,Map<K,E>,E,K> builder = (PluralAttributeBuilder<D,Map<K,E>,E,K>) this;
			return (AbstractPluralAttribute<D, C, E>) new MapAttributeImpl<>(
					builder
			);
		}
		else if ( Set.class.isAssignableFrom( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D,Set<E>, E,?> builder = (PluralAttributeBuilder<D, Set<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new SetAttributeImpl<>(
					builder
			);
		}
		else if ( List.class.isAssignableFrom( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D, List<E>, E,?> builder = (PluralAttributeBuilder<D, List<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new ListAttributeImpl<>(
					builder
			);
		}
		else if ( Collection.class.isAssignableFrom( collectionJavaTypeDescriptor.getJavaType() ) ) {
			final PluralAttributeBuilder<D, Collection<E>,E,?> builder = (PluralAttributeBuilder<D, Collection<E>, E,?>) this;
			return (AbstractPluralAttribute<D, C, E>) new BagAttributeImpl<>(
					builder
			);
		}
		throw new UnsupportedOperationException( "Unknown collection: " + collectionJavaTypeDescriptor.getJavaType() );
	}
}
