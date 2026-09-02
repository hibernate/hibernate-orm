/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.schema.internal.StandardSchemaCommentSupport;

import static org.hibernate.SPI.Role.USE;

/// Stock database-object comment profiles for schema export.
///
/// Use these immutable profiles when their placement and grammar match the
/// database. Implement [SchemaCommentSupport] for a different grammar.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class SchemaCommentSupports {
	private SchemaCommentSupports() {
	}

	public static SchemaCommentSupport none() {
		return StandardSchemaCommentSupport.NONE;
	}

	public static SchemaCommentSupport commentOn() {
		return StandardSchemaCommentSupport.COMMENT_ON;
	}

	public static SchemaCommentSupport hanaInline() {
		return StandardSchemaCommentSupport.HANA_INLINE;
	}

	public static SchemaCommentSupport mysqlInline() {
		return StandardSchemaCommentSupport.MYSQL_INLINE;
	}
}
