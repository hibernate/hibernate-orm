/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.dialect.MySQLDialect;

import static org.hibernate.type.SqlTypes.ORDINAL_ENUM;

/**
 * Represents an {@code enum} type for databases like MySQL and H2.
 * <p>
 * Hibernate will automatically use this for enums mapped
 * as {@link jakarta.persistence.EnumType#ORDINAL}.
 *
 * @see org.hibernate.type.SqlTypes#ORDINAL_ENUM
 * @see MySQLDialect#getEnumTypeDeclaration(String, String[])
 */
public class OrdinalEnumJdbcType extends EnumJdbcType {

	public static final OrdinalEnumJdbcType INSTANCE = new OrdinalEnumJdbcType();

	@Override
	public int getDefaultSqlTypeCode() {
		return ORDINAL_ENUM;
	}

}
