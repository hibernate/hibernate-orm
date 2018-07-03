/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import javax.persistence.metamodel.EntityType;

/**
 * @author Steve Ebersole
 */
public interface Metamodel extends javax.persistence.metamodel.Metamodel {
	/**
	 * Access to the SessionFactory that this Metamodel instance is bound to.
	 *
	 * @return The SessionFactory
	 */
	SessionFactory getSessionFactory();

	/**
	 * @deprecated since 5.2
	 */
	@Deprecated
	default EntityType getEntityTypeByName(String entityName) {
		return entity( entityName );
	}

	/**
	 * Access to an entity supporting Hibernate's entity-name feature
	 *
	 * @param entityName The entity-name
	 *
	 * @return The entity descriptor
	 */
	<X> EntityType<X> entity(String entityName);

	String getImportedClassName(String className);

	/**
	 * Given the name of an entity class, determine all the class and interface names by which it can be
	 * referenced in an HQL query.
	 *
	 * @param entityName The name of the entity class
	 *
	 * @return the names of all persistent (mapped) classes that extend or implement the
	 *     given class or interface, accounting for implicit/explicit polymorphism settings
	 *     and excluding mapped subclasses/joined-subclasses of other classes in the result.
	 * @throws MappingException
	 */
	String[] getImplementors(String entityName);

}
