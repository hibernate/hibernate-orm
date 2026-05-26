/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	@Nonnull
	JpaMetamodel getMetamodel();

	/**
	 * The representation mode.
	 *
	 * @return {@link RepresentationMode#POJO POJO} for Java class entities,
	 *         or {@link RepresentationMode#MAP MAP} for dynamic entities.
	 */
	@Nonnull
	RepresentationMode getRepresentationMode();

	/**
	 * The Java class of the entity type.
	 */
	@Override
	@Nonnull
	default Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	/**
	 * The descriptor of the supertype of this type.
	 */
	@Nullable
	ManagedDomainType<? super J> getSuperType();

	/**
	 * The descriptors of all known managed subtypes of this type.
	 */
	@Nonnull
	Collection<? extends ManagedDomainType<? extends J>> getSubTypes();

	@Internal
	void addSubType(@Nonnull ManagedDomainType<? extends J> subType);

	void visitAttributes(@Nonnull Consumer<? super PersistentAttribute<? super J, ?>> action);
	void visitDeclaredAttributes(@Nonnull Consumer<? super PersistentAttribute<J, ?>> action);

	@Override
	@Nonnull
	PersistentAttribute<? super J,?> getAttribute(@Nonnull String name);

	@Override
	@Nonnull
	PersistentAttribute<J,?> getDeclaredAttribute(@Nonnull String name);

	@Nullable
	PersistentAttribute<? super J,?> findAttribute(@Nonnull String name);

	@Nullable
	PersistentAttribute<?, ?> findSubTypesAttribute(@Nonnull String name);

	@Nullable
	SingularPersistentAttribute<? super J,?> findSingularAttribute(@Nonnull String name);
	@Nullable
	PluralPersistentAttribute<? super J, ?,?> findPluralAttribute(@Nonnull String name);
	@Nullable
	PersistentAttribute<? super J, ?> findConcreteGenericAttribute(@Nonnull String name);

	@Nullable
	PersistentAttribute<J,?> findDeclaredAttribute(@Nonnull String name);
	@Nullable
	SingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(@Nonnull String name);
	@Nullable
	PluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(@Nonnull String name);
	@Nullable
	PersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(@Nonnull String name);
}
