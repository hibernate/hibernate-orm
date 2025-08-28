/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.Internal;
import org.hibernate.metamodel.RepresentationMode;

import jakarta.persistence.metamodel.ManagedType;

/**
 * Extensions to the JPA-defined {@link ManagedType} contract.
 *
 * @author Steve Ebersole
 */
public interface ManagedDomainType<J> extends DomainType<J>, ManagedType<J> {
	/**
	 * The name of the managed type.
	 *
	 * @apiNote This usually returns the name of the Java class. However, for
	 *          {@linkplain RepresentationMode#MAP dynamic models}, this returns
	 *          the symbolic name since the Java type is {@link java.util.Map}.
	 *
	 * @see #getRepresentationMode()
	 */
	@Override
	String getTypeName();

	/**
	 * The parent {@linkplain JpaMetamodel metamodel}.
	 */
	JpaMetamodel getMetamodel();

	/**
	 * The representation mode.
	 *
	 * @return {@link RepresentationMode#POJO POJO} for Java class entities,
	 *         or {@link RepresentationMode#MAP MAP} for dynamic entities.
	 */
	RepresentationMode getRepresentationMode();

	/**
	 * The Java class of the entity type.
	 */
	@Override
	default Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	/**
	 * The descriptor of the supertype of this type.
	 */
	ManagedDomainType<? super J> getSuperType();

	/**
	 * The descriptors of all known managed subtypes of this type.
	 */
	Collection<? extends ManagedDomainType<? extends J>> getSubTypes();

	@Internal
	void addSubType(ManagedDomainType<? extends J> subType);

	void visitAttributes(Consumer<? super PersistentAttribute<? super J, ?>> action);
	void visitDeclaredAttributes(Consumer<? super PersistentAttribute<J, ?>> action);

	@Override
	PersistentAttribute<? super J,?> getAttribute(String name);

	@Override
	PersistentAttribute<J,?> getDeclaredAttribute(String name);

	PersistentAttribute<? super J,?> findAttribute(String name);

	PersistentAttribute<?, ?> findSubTypesAttribute(String name);

	/**
	 * @deprecated Use {@link #findAttribute(String)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	default PersistentAttribute<? super J, ?> findAttributeInSuperTypes(String name) {
		return findAttribute( name );
	}

	SingularPersistentAttribute<? super J,?> findSingularAttribute(String name);
	PluralPersistentAttribute<? super J, ?,?> findPluralAttribute(String name);
	PersistentAttribute<? super J, ?> findConcreteGenericAttribute(String name);

	PersistentAttribute<J,?> findDeclaredAttribute(String name);
	SingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(String name);
	PluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(String name);
	PersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(String name);
}
