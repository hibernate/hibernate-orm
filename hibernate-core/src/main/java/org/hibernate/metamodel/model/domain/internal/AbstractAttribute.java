/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Member;

import jakarta.persistence.metamodel.Attribute;

import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.tree.from.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Models the commonality of the JPA {@link Attribute} hierarchy.
 *
 * @param <D> The type of the class (D)eclaring this attribute
 * @param <J> The (J)ava type of this attribute
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttribute<D,J,B> implements PersistentAttribute<D,J>, Serializable {
	private final ManagedDomainType<D> declaringType;
	private final String name;
	private final JavaType<J> attributeJtd;

	private final AttributeClassification attributeClassification;

	private final SqmDomainType<B> valueType;
	private final transient Member member;

	protected AbstractAttribute(
			ManagedDomainType<D> declaringType,
			String name,
			JavaType<J> attributeJtd,
			AttributeClassification attributeClassification,
			SqmDomainType<B> valueType,
			Member member) {
		this.declaringType = declaringType;
		this.name = name;
		this.attributeJtd = attributeJtd;
		this.attributeClassification = attributeClassification;
		this.valueType = valueType;
		this.member = member;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<J> getJavaType() {
		return valueType instanceof BasicTypeImpl basicType
				? basicType.getJavaType()
				: attributeJtd.getJavaTypeClass();
	}

	public SqmDomainType<B> getSqmPathType() {
		return valueType;
	}

	@Override
	public JavaType<J> getAttributeJavaType() {
		return attributeJtd;
	}

	@Override
	public ManagedDomainType<D> getDeclaringType() {
		return declaringType;
	}

	@Override
	public Member getJavaMember() {
		return member;
	}

	@Override
	public AttributeClassification getAttributeClassification() {
		return attributeClassification;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return getAttributeClassification().getJpaClassification();
	}

	@Override
	public DomainType<?> getValueGraphType() {
		return valueType;
	}

	@Override
	public String toString() {
		return declaringType.getTypeName() + '#' + name + '(' + attributeClassification + ')';
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

	protected Object writeReplace() throws ObjectStreamException {
		return new SerialForm( declaringType, name );
	}

	private static class SerialForm implements Serializable {
		private final ManagedDomainType<?> declaringType;
		private final String name;

		public SerialForm(ManagedDomainType<?> declaringType, String name) {
			this.declaringType = declaringType;
			this.name = name;
		}

		private Object readResolve() {
			return declaringType.findAttribute( name );
		}

	}
}
