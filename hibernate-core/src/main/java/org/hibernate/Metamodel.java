/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.metamodel.model.domain.JpaMetamodel;

/**
 * @author Steve Ebersole
 *
 * @deprecated (since 6.0) Prefer {@link JpaMetamodel}
 *
 * @see JpaMetamodel
 */
@Deprecated
public interface Metamodel extends JpaMetamodel {
	/**
	 * Access to the SessionFactory that this Metamodel instance is bound to.
	 *
	 * @return The SessionFactory
	 */
	SessionFactory getSessionFactory();

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
