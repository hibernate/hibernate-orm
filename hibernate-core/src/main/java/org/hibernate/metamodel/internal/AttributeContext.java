/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Bundle's a Hibernate property mapping together with the JPA metamodel information
 * of the attribute owner.
 *
 * @param <X> The owner type.
 */
public interface AttributeContext<X> {
	/**
	 * Retrieve the attribute owner.
	 *
	 * @return The owner.
	 */
	ManagedDomainType<X> getOwnerType();

	/**
	 * Retrieve the Hibernate property mapping.
	 *
	 * @return The Hibernate property mapping.
	 */
	Property getPropertyMapping();
}
