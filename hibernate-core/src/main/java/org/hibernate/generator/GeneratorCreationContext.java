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
import org.hibernate.mapping.RootClass;
import org.hibernate.service.ServiceRegistry;

/**
 * An object passed as a parameter to the constructor or
 * {@link AnnotationBasedGenerator#initialize initialize}
 * method of a {@link Generator} which provides access to
 * certain objects useful for initialization of the
 * generator.
 *
 * @since 6.2
 */
@Incubating
public interface GeneratorCreationContext {
	Database getDatabase();
	ServiceRegistry getServiceRegistry();

	String getDefaultCatalog();
	String getDefaultSchema();

	PersistentClass getPersistentClass();
	RootClass getRootClass();

	Property getProperty();
}
