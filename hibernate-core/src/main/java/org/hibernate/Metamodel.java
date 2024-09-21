/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.metamodel.model.domain.JpaMetamodel;

/**
 * @author Steve Ebersole
 *
 * @deprecated Prefer {@link JpaMetamodel}
 *
 * @see JpaMetamodel
 */
@Deprecated(since = "6.0")
public interface Metamodel extends JpaMetamodel {
	/**
	 * Given the name of an entity class, determine all the class and interface names by which it can be
	 * referenced in an HQL query.
	 *
	 * @param entityName The name of the entity class
	 *
	 * @return the names of all persistent (mapped) classes that extend or implement the
	 *     given class or interface, accounting for implicit/explicit polymorphism settings
	 *     and excluding mapped subclasses/joined-subclasses of other classes in the result.
	 */
	String[] getImplementors(String entityName);

}
