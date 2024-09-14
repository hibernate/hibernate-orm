/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.env.internal;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;

import org.hibernate.type.SqlTypes;

/**
 * Enumeration of the JDBC LOB locator types
 *
 * @author Steve Ebersole
 */
public enum LobTypes {
	BLOB( SqlTypes.BLOB, Blob.class ),
	CLOB( SqlTypes.CLOB, Clob.class ),
	NCLOB( SqlTypes.NCLOB, NClob.class );

	private final int jdbcTypeCode;
	private final Class<?> jdbcTypeClass;

	LobTypes(int jdbcTypeCode, Class<?> jdbcTypeClass) {
		this.jdbcTypeCode = jdbcTypeCode;
		this.jdbcTypeClass = jdbcTypeClass;
	}

	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	public Class<?> getJdbcTypeClass() {
		return jdbcTypeClass;
	}
}
