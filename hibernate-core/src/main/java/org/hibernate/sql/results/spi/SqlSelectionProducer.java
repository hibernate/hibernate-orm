/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface SqlSelectionProducer {
	/**
	 * Create a SqlSelection for the given JDBC ResultSet position
	 *  @param jdbcPosition The position within the SQL selections
	 * @param valuesArrayPosition
	 * @param javaTypeDescriptor The descriptor for the Java type to read the value as
	 * @param typeConfiguration The associated TypeConfiguration
	 */
	SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition, BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration);
}
