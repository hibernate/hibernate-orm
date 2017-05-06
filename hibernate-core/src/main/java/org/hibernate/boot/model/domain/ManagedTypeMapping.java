/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;

/**
 * Corollary to what JPA calls a "managed type" as part of Hibernate's boot-time
 * metamodel.  Essentially a base class describing the commonality between an entity,
 * a mapped-superclass and an embeddable.
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeMapping {
	/**
	 * The name of this managed type.  Generally the class name.
	 */
	String getName();

	/**
	 * The ordering here is defined by the alphabetical ordering of the
	 * attributes' names
	 *
	 * todo (6.0) : is this what we want?
	 */
	List<PersistentAttributeMapping> getDeclaredPersistentAttributes();
}
