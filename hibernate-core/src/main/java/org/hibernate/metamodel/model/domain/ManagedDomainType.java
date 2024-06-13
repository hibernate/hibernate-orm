/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.query.sqm.SqmExpressible;

import jakarta.persistence.metamodel.ManagedType;

/**
 * Extensions to the JPA-defined {@link ManagedType} contract.
 *
 * @author Steve Ebersole
 */
public interface ManagedDomainType<J> extends SqmExpressible<J>, DomainType<J>, ManagedType<J> {
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

	RepresentationMode getRepresentationMode();

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
	PersistentAttribute<? super J, ?> findAttributeInSuperTypes(String name);

	SingularPersistentAttribute<? super J,?> findSingularAttribute(String name);
	PluralPersistentAttribute<? super J, ?,?> findPluralAttribute(String name);
	PersistentAttribute<? super J, ?> findConcreteGenericAttribute(String name);

	PersistentAttribute<J,?> findDeclaredAttribute(String name);
	SingularPersistentAttribute<J, ?> findDeclaredSingularAttribute(String name);
	PluralPersistentAttribute<J, ?, ?> findDeclaredPluralAttribute(String name);
	PersistentAttribute<J, ?> findDeclaredConcreteGenericAttribute(String name);
}
