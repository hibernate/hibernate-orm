/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface SubclassTypesAware<T> extends ManagedTypeImplementor<T> {
	// todo (6.0) : List, or Set?  The order would be non-guaranteed
	List<ManagedTypeImplementor<? extends T>> getSubclassTypes();

	void addSubclassType(ManagedTypeImplementor<? extends T> subclassType);
}
