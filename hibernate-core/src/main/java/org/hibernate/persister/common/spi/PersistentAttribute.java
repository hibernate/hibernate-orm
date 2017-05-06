/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

/**
 * Models a persistent (mapped) attribute in Hibernate's "runtime model".
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute<O,T>
		extends Navigable<T>, TypeExporter<T>, javax.persistence.metamodel.Attribute<O,T> {
	@Override
	ManagedTypeImplementor<O> getSource();

	default String getAttributeName() {
		return getNavigableName();
	}

	@Override
	default String getName() {
		return getNavigableRole().getNavigableName();
	}

	@Override
	default ManagedTypeImplementor<O> getDeclaringType() {
		return getSource();
	}

	@Override
	@SuppressWarnings("unchecked")
	default Class<T> getJavaType() {
		return (Class<T>) getJavaTypeDescriptor().getJavaType();
	}
}
