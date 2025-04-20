/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.metamodel.RepresentationMode;

import jakarta.persistence.metamodel.ManagedType;
import org.hibernate.query.BindableType;

/**
 * Extensions to the JPA-defined {@link ManagedType} contract.
 *
 * @author Steve Ebersole
 */
public interface ManagedDomainType<J>
		extends DomainType<J>, ManagedType<J>, BindableType<J> {
	/**
	 * Get the type name.
	 *
	 * @apiNote This usually returns the name of the Java class. However, for
	 *          {@linkplain RepresentationMode#MAP dynamic models}, this returns
	 *          the symbolic name since the Java type is {@link java.util.Map}.
	 *
	 * @return The type name.
	 *
	 * @see #getRepresentationMode()
	 */
	String getTypeName();

	JpaMetamodel getMetamodel();

	RepresentationMode getRepresentationMode();

	@Override
	default Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	/**
	 * The descriptor of the supertype of this type.
	 */
	ManagedDomainType<? super J> getSuperType();

	Collection<? extends ManagedDomainType<? extends J>> getSubTypes();

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
