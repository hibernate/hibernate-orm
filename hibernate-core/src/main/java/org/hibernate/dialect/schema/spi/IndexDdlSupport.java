/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Supplies index creation grammar and name qualification policy.
///
/// Consume rendered [IndexColumn] views without retaining the request or
/// depending on mutable mapping objects.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getIndexDdlSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface IndexDdlSupport {
	default String createCommand(IndexDdlRequest request) {
		return requireNonNull( request ).unique() ? "create unique index" : "create index";
	}

	default String createTail(IndexDdlRequest request) {
		requireNonNull( request );
		return "";
	}

	default IndexNameQualification nameQualification() {
		return IndexNameQualification.QUALIFIED;
	}
}
