/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface PersistentAttribute<O,T>
		extends Navigable<T>, TypeExporter<T>, javax.persistence.metamodel.Attribute<O,T> {
	@Override
	ManagedTypeImplementor<O> getSource();

	String getAttributeName();

	@Override
	default String getName() {
		return getNavigableName();
	}

	@Override
	default ManagedTypeImplementor<O> getDeclaringType() {
		return getSource();
	}

	@Override
	default Type getExportedDomainType() {
		return getOrmType();
	}

	@Override
	@SuppressWarnings("unchecked")
	default Class<T> getJavaType() {
		return (Class<T>) getExportedDomainType().getJavaTypeDescriptor().getJavaType();
	}
}
