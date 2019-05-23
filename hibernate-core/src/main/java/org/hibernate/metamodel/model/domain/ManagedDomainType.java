/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import java.util.function.Consumer;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.RepresentationMode;

/**
 * Hibernate extension to the JPA {@link ManagedType} contract
 *
 * @author Steve Ebersole
 */
public interface ManagedDomainType<J> extends SimpleDomainType<J>, ManagedType<J> {
	/**
	 * Get the type name.
	 *
	 * Generally speaking, this returns the name of the Java class.  However, for
	 * dynamic models ({@link RepresentationMode#MAP}), this returns the symbolic name
	 * since the Java type is {@link java.util.Map}
	 *
	 * @return The type name.
	 *
	 * @see #getRepresentationMode()
	 */
	String getTypeName();

	RepresentationMode getRepresentationMode();

	/**
	 * This type's super type descriptor.  Note : we define this on the managed
	 * type descriptor in anticipation of supporting embeddable inheritance
	 */
	ManagedDomainType<? super J> getSuperType();

	void visitAttributes(Consumer<PersistentAttribute<? super J,?>> action);
	void visitDeclaredAttributes(Consumer<PersistentAttribute<J,?>> action);

	@Override
	PersistentAttribute<? super J,?> getAttribute(String name);

	@Override
	PersistentAttribute<J,?> getDeclaredAttribute(String name);

	PersistentAttribute<? super J,?> findAttribute(String name);
	SingularPersistentAttribute<? super J,?> findSingularAttribute(String name);
	PluralPersistentAttribute<? super J, ?,?> findPluralAttribute(String name);

	PersistentAttribute<J,?> findDeclaredAttribute(String name);
	SingularPersistentAttribute<? super J, ?> findDeclaredSingularAttribute(String name);
	PluralPersistentAttribute<? super J, ?, ?> findDeclaredPluralAttribute(String name);

	SubGraphImplementor<J> makeSubGraph();
	<S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subClassType);

	<S extends J> ManagedDomainType<S> findSubType(String subTypeName);
	<S extends J> ManagedDomainType<S> findSubType(Class<S> subType);
}