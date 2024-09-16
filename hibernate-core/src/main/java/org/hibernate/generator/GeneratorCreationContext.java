/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.ServiceRegistry;

/**
 * Access to information useful during {@linkplain Generator} creation and initialization.
 *
 * @see AnnotationBasedGenerator
 * @see org.hibernate.id.Configurable#create(GeneratorCreationContext)
 */
@Incubating
public interface GeneratorCreationContext {
	/**
	 * View of the relational database objects (tables, sequences, ...) and namespaces (catalogs and schemas).
	 */
	Database getDatabase();

	/**
	 * Access to available services.
	 */
	ServiceRegistry getServiceRegistry();

	/**
	 * The default catalog name, if one.
	 */
	String getDefaultCatalog();

	/**
	 * The default schema name, if one.
	 */
	String getDefaultSchema();

	/**
	 * Mapping details for the entity; may be null in the case of and id-bag id generator.
	 */
	PersistentClass getPersistentClass();

	/**
	 * The entity identifier or id-bag property details.
	 */
	Property getProperty();
}
