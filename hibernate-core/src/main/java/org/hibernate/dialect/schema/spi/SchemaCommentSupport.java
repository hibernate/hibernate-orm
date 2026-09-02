/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines comment placement and rendering for database schema objects.
///
/// This contract applies only to DDL comments attached to tables, columns, and
/// user-defined types. It does not control comments embedded in SQL queries,
/// query hints, SQL AST decoration, or statement inspection.
///
/// Return a non-null placement for every [CommentTarget] and render the complete
/// inline fragment or standalone command described by that placement.
///
/// @see Dialect#getSchemaCommentSupport()
///
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface SchemaCommentSupport {
	CommentPlacement placement(CommentTarget target);

	String render(CommentRequest request);
}
