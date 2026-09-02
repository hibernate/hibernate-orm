/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.queryhint.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies the final position of a leading database hint relative to a user comment.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum QueryHintPlacement {
	/// Place the database hint before the user comment.
	BEFORE_COMMENT,
	/// Place the database hint after the user comment.
	AFTER_COMMENT
}
