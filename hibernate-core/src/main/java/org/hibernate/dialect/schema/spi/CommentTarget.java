/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies the database object receiving a schema-export comment.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum CommentTarget {
	TABLE,
	TABLE_COLUMN,
	USER_DEFINED_TYPE,
	USER_DEFINED_TYPE_COLUMN
}
