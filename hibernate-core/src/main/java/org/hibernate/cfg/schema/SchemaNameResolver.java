/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.schema;

/**
 * Resolve schema name from entity class.
 *
 * @author Benoit Besson
 */
public interface SchemaNameResolver {

	public static String EMPTY_SCHEMA_NAME = "";

	/** get Schema name from entity class. */
	String getSchema(Class<?> clazz);

}
