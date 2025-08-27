/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.internal.PluralAttributeMetadata;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.metamodel.internal.AttributeFactory.determineSimpleType;

/**
 * A "parameter object" for creating a plural attribute
 */
public class PluralAttributeBuilder<D, C, E, K> {
	private final JavaType<C> collectionJtd;
	private final boolean isGeneric;

	private final AttributeClassification attributeClassification;
	private final CollectionClassification collectionClassification;

	private final SqmDomainType<E> elementType;
	private final DomainType<K> listIndexOrMapKeyType;

	private final ManagedDomainType<D> declaringType;

	private final Property property;
	private final Member member;

	public PluralAttributeBuilder(
			JavaType<C> collectionJtd,
			boolean isGeneric,
			AttributeClassification attributeClassification,
			CollectionClassification collectionClassification,
			SqmDomainType<E> elementType,
			DomainType<K> listIndexOrMapKeyType,
			ManagedDomainType<D> declaringType,
			Property property,
			Member member) {
		this.collectionJtd = collectionJtd;
		this.isGeneric = isGeneric;
		this.attributeClassification = attributeClassification;
		this.collectionClassification = collectionClassification;
		this.elementType = elementType;
		this.listIndexOrMapKeyType = listIndexOrMapKeyType;
		this.declaringType = declaringType;
		this.property = property;
		this.member = member;
	}

	public static <Y, X> PersistentAttribute<X, Y> build(
			PluralAttributeMetadata<?,Y,?> attributeMetadata,
			boolean isGeneric,
			MetadataContext metadataContext) {

		final JavaType<Y> attributeJtd =
				metadataContext.getTypeConfiguration().getJavaTypeRegistry()
						.getDescriptor( attributeMetadata.getJavaType() );

		final var builder = new PluralAttributeBuilder<>(
				attributeJtd,
				isGeneric,
				attributeMetadata.getAttributeClassification(),
				attributeMetadata.getCollectionClassification(),
				(SqmDomainType<?>) // TODO: this typecast is very ugly and fragile
						determineSimpleType( attributeMetadata.getElementValueContext(), metadataContext ),
				determineListIndexOrMapKeyType( attributeMetadata, metadataContext ),
				attributeMetadata.getOwnerType(),
				attributeMetadata.getPropertyMapping(),
				attributeMetadata.getMember()
		);

		final Class<Y> javaClass = attributeJtd.getJavaTypeClass();
		if ( Map.class.equals( javaClass ) ) {
			return new MapAttributeImpl( builder );
		}
		else if ( Set.class.equals( javaClass ) ) {
			return new SetAttributeImpl( builder );
		}
		else if ( List.class.equals( javaClass ) ) {
			return new ListAttributeImpl( builder );
		}
		else if ( Collection.class.equals( javaClass ) ) {
			return new BagAttributeImpl( builder );
		}

		//apply loose rules
		if ( javaClass.isArray() ) {
			return new ListAttributeImpl( builder );
		}

		if ( Map.class.isAssignableFrom( javaClass ) ) {
			return new MapAttributeImpl( builder );
		}
		else if ( Set.class.isAssignableFrom( javaClass ) ) {
			return new SetAttributeImpl( builder );
		}
		else if ( List.class.isAssignableFrom( javaClass ) ) {
			return new ListAttributeImpl( builder );
		}
		else if ( Collection.class.isAssignableFrom( javaClass ) ) {
			return new BagAttributeImpl( builder );
		}

		throw new UnsupportedMappingException( "Unknown collection: " + attributeJtd.getJavaType() );
	}

	private static SimpleDomainType<?> determineListIndexOrMapKeyType(
			PluralAttributeMetadata<?,?,?> attributeMetadata,
			MetadataContext metadataContext) {
		final Class<?> javaType = attributeMetadata.getJavaType();
		if ( Map.class.isAssignableFrom( javaType ) ) {
			return (SimpleDomainType<?>)
					determineSimpleType( attributeMetadata.getMapKeyValueContext(), metadataContext );
		}

		if ( List.class.isAssignableFrom( javaType ) || javaType.isArray() ) {
			return metadataContext.getTypeConfiguration().getBasicTypeRegistry()
					.getRegisteredType( Integer.class );
		}

		return null;
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

	public DomainType<K> getListIndexOrMapKeyType() {
		return listIndexOrMapKeyType;
	}

	public JavaType<C> getCollectionJavaType() {
		return collectionJtd;
	}

	public boolean isGeneric() {
		return isGeneric;
	}

	public SqmDomainType<E> getValueType() {
		return elementType;
	}

	public Property getProperty() {
		return property;
	}

	public Member getMember() {
		return member;
	}
}
