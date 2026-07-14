/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Member;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.Attribute;

import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.sqm.tree.spi.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPersistentAttribute;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildParentNavigablePath;

/**
 * Models the commonality of the JPA {@link Attribute} hierarchy.
 *
 * @param <D> The type of the class (D)eclaring this attribute
 * @param <J> The (J)ava type of this attribute
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttribute<D,J,B>
		implements SqmPersistentAttribute<D,J>, Serializable {
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
	@Nonnull
	public String getName() {
		return name;
	}

	@Override
	@Nonnull
	public Class<J> getJavaType() {
		//noinspection unchecked
		return (Class<J>) valueType.getJavaType();
	}

	public SqmDomainType<B> getPathType() {
		return valueType;
	}

	@Override
	@Nonnull
	public JavaType<J> getAttributeJavaType() {
		return attributeJtd;
	}

	@Override
	@Nonnull
	public ManagedDomainType<D> getDeclaringType() {
		return declaringType;
	}

	@Override
	@Nonnull
	public Member getJavaMember() {
		return member;
	}

	@Override
	@Nonnull
	public AttributeClassification getAttributeClassification() {
		return attributeClassification;
	}

	@Override
	@Nonnull
	public PersistentAttributeType getPersistentAttributeType() {
		final var classification = getAttributeClassification().getJpaClassification();
		if ( classification == null ) {
			throw new IllegalStateException( "Non-JPA classification: " + attributeClassification );
		}
		return classification;
	}

	@Override
	@Nonnull
	public DomainType<?> getValueGraphType() {
		return valueType;
	}

	NavigablePath getParentNavigablePath(SqmPath<?> parent) {
		final var parentPathSource = parent.getResolvedModel();
		final var parentType = parentPathSource.getPathType();
		final var parentNavigablePath = buildParentNavigablePath( parent, "" );
		if ( parentType != declaringType
				&& parentType instanceof EntityDomainType<?> entityDomainType
				&& entityDomainType.findAttribute( name ) == null ) {
			// If the parent path is an entity type which does not contain the
			// joined attribute add an implicit treat to the parent's navigable path
			return parentNavigablePath.treatAs( declaringType.getTypeName() );
		}
		else {
			return parentNavigablePath;
		}
	}

	@Override
	public String toString() {
		return declaringType.getTypeName() + '#' + name + '(' + attributeClassification + ')';
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

	@Serial
	protected Object writeReplace() throws ObjectStreamException {
		return new SerialForm( declaringType, name );
	}

	private record SerialForm(ManagedDomainType<?> declaringType, String name)
			implements Serializable {
		@Serial
		private Object readResolve() {
			return declaringType.findAttribute( name );
		}

	}
}
