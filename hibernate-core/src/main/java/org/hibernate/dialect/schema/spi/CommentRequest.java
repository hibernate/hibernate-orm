/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.internal.util.StringHelper.isBlank;

/// Describes a comment attached to one database schema object.
///
/// The qualified name is already formatted for the database. The raw comment
/// may be empty and is escaped by the selected [SchemaCommentSupport].
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record CommentRequest(
		CommentTarget target,
		String qualifiedName,
		String comment) {
	public CommentRequest {
		requireNonNull( target, "target" );
		if ( isBlank( qualifiedName ) ) {
			throw new IllegalArgumentException( "qualifiedName must not be blank" );
		}
		requireNonNull( comment, "comment" );
	}
}
