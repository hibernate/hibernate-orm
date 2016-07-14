/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import javax.persistence.metamodel.EntityType;

import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines the Hibernate API extension of the JPA {@link javax.persistence.metamodel.Metamodel}
 * API.
 *
 * @author Steve Ebersole
 */
public interface Metamodel extends javax.persistence.metamodel.Metamodel {
	/**
	 * Access to the TypeConfiguration in effect for this SessionFactory/Metamodel
	 *
	 * @return Access to the TypeConfiguration
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Access to an entity supporting Hibernate's entity-name feature
	 *
	 * @param entityName The entity-name
	 *
	 * @return The entity descriptor
	 */
	<X> EntityType<X> entity(String entityName);

	/**
	 * See if the given name (typically encountered in an HQL/JPQL query) has an imported/replacement name.
	 *
	 * @param className The encountered class/entity name
	 *
	 * @return The imported class name.  May return {@code null} if the given name does not have an import
	 * replacement and it does not represent a known Class name (Class loading).
	 */
	String getImportedClassName(String className);

	/**
	 * Get the names of all persistent classes that implement/extend the given interface/class
	 */
	String[] getImplementors(String entityName);
}
