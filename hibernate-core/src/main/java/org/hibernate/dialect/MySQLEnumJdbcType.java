/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.type.descriptor.jdbc.EnumJdbcType;

/**
 * Represents an {@code enum} type on MySQL.
 * <p>
 * Hibernate will automatically use this for enums mapped
 * as {@link jakarta.persistence.EnumType#STRING}.
 *
 * @see org.hibernate.type.SqlTypes#ENUM
 * @see MySQLDialect#getEnumTypeDeclaration(String, String[])
 * @deprecated Use {@link EnumJdbcType} instead
 *
 * @author Gavin King
 */
@Deprecated(forRemoval = true)
public class MySQLEnumJdbcType extends EnumJdbcType {
}
