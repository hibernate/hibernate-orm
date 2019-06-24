/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface BasicJavaDescriptor<T> extends JavaTypeDescriptor<T> {
	/**
	 * Obtain the {@link VersionSupport} for this Java type.
	 * <p/>
	 *
	 * @return The {@link VersionSupport} or null if this Java type does not support version
	 */
	default VersionSupport<T> getVersionSupport() {
		return null;
	}

	/**
	 * The check constraint that should be added to the column
	 * definition in generated DDL.
	 *
	 * @param columnName the name of the column
	 * @param sqlTypeDescriptor the {@link SqlTypeDescriptor}
	 *                          for the mapped column
	 * @param dialect the SQL {@link Dialect}
	 * @return a check constraint condition or null
	 */
	default String getCheckCondition(String columnName, SqlTypeDescriptor sqlTypeDescriptor, Dialect dialect) {
		return null;
	}
}
