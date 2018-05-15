/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql;

import java.io.Serializable;

/**
 * Describes a JDBC/SQL type.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor extends Serializable {
	/**
	 * Return the {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
	 *
	 * @return typeCode The JDBC type-code
	 *
	 * @deprecated (since 6.0) Use {@link #getJdbcTypeCode} instead.
	 */
	@Deprecated
	default int getSqlType(){
		return getJdbcTypeCode();
	}

	/**
	 * Retrieve the JDBC/SQL type-code that this descriptor represents.
	 * <p/>
	 * For a "standard" type that would match the corresponding value in
	 * {@link java.sql.Types}.
	 *
	 * @return typeCode The JDBC/SQL type-code
	 */
	int getJdbcTypeCode();
}
