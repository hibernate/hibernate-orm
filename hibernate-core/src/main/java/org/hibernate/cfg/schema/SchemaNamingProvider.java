/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.schema;

import org.hibernate.annotations.common.reflection.XClass;

/**
 * A schema naming provider used to dynamically set schema name for all tables and sequences.
 *
 * @author Benoit Besson
 */
public interface SchemaNamingProvider {

	// called each time the processing class has changed
	public void setCurrentProcessingClass(XClass xClass);

	// ask for dynamic schema name for table
	public String resolveSchemaName(String annotationSchemaName, String annotationTableName);

	// ask for dynamic schema name for sequence
	public String resolveSequenceName(String annotationSequenceName);
}
