/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.EnumType;

import static org.hibernate.type.SqlTypes.NAMED_ORDINAL_ENUM;

/**
 * Represents a named {@code enum} type on GaussDB.
 * <p>
 * Hibernate does <em>not</em> automatically use this for enums
 * mapped as {@link EnumType#ORDINAL}, and
 * instead this type must be explicitly requested using:
 * <pre>
 * &#64;JdbcTypeCode(SqlTypes.NAMED_ORDINAL_ENUM)
 * </pre>
 *
 * @see org.hibernate.type.SqlTypes#NAMED_ORDINAL_ENUM
 * @see GaussDBDialect#getEnumTypeDeclaration(String, String[])
 * @see GaussDBDialect#getCreateEnumTypeCommand(String, String[])
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLOrdinalEnumJdbcType.
 */
public class GaussDBOrdinalEnumJdbcType extends GaussDBEnumJdbcType {

	public static final GaussDBOrdinalEnumJdbcType INSTANCE = new GaussDBOrdinalEnumJdbcType();

	@Override
	public int getDefaultSqlTypeCode() {
		return NAMED_ORDINAL_ENUM;
	}
}
