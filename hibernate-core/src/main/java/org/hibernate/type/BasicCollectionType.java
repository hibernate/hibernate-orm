/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.util.Collection;
import java.util.Objects;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import static org.hibernate.type.BasicArrayType.determineElementTypeName;

/**
 * A type that maps between {@link java.sql.Types#ARRAY ARRAY} and {@code Collection<T>}
 *
 * @author Christian Beikov
 */
public class BasicCollectionType<C extends Collection<E>, E>
		extends AbstractSingleColumnStandardBasicType<C>
		implements AdjustableBasicType<C>, BasicPluralType<C, E> {

	private final BasicType<E> baseDescriptor;
	private final String name;

	public BasicCollectionType(
			BasicType<E> baseDescriptor,
			JdbcType arrayJdbcType,
			BasicCollectionJavaType<C, E> collectionTypeDescriptor) {
		super( arrayJdbcType, collectionTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = determineName( collectionTypeDescriptor, baseDescriptor );
	}

	public BasicCollectionType(
			BasicType<E> baseDescriptor,
			JdbcType arrayJdbcType,
			JavaType<C> collectionTypeDescriptor,
			String typeName) {
		super( arrayJdbcType, collectionTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = typeName;
	}

	private static String determineName(BasicCollectionJavaType<?, ?> collectionTypeDescriptor, BasicType<?> baseDescriptor) {
		final String elementTypeName = determineElementTypeName( baseDescriptor );
		switch ( collectionTypeDescriptor.getSemantics().getCollectionClassification() ) {
			case BAG:
			case ID_BAG:
				return "Collection<" + elementTypeName + ">";
			case LIST:
				return "List<" + elementTypeName + ">";
			case SET:
				return "Set<" + elementTypeName + ">";
			case SORTED_SET:
				return "SortedSet<" + elementTypeName + ">";
			case ORDERED_SET:
				return "OrderedSet<" + elementTypeName + ">";
		}
		return null;
	}

	@Override
	public BasicType<E> getElementType() {
		return baseDescriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> domainJtd) {
		// TODO: maybe fallback to some encoding by default if the DB doesn't support arrays natively?
		//  also, maybe move that logic into the ArrayJdbcType
		//noinspection unchecked
		return (BasicType<X>) this;
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o.getClass() == BasicCollectionType.class
				&& Objects.equals( baseDescriptor, ( (BasicCollectionType<?, ?>) o ).baseDescriptor );
	}

	@Override
	public int hashCode() {
		return baseDescriptor.hashCode();
	}
}
