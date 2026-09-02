/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence.spi;

import org.hibernate.MappingException;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Standard family base for databases using the ANSI `next value for`
/// expression and ordinary create/drop sequence grammar.
///
/// Extend this class only when the database retains the ANSI next-value
/// expression. Override the non-final previous-value or DDL methods for the
/// database-specific differences and delegate unchanged behavior to `super`.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT })
public class ANSISequenceSupport implements SequenceSupport {
	/// Create an ANSI sequence-support strategy or provider subclass.
	@SPI(IMPLEMENT)
	public ANSISequenceSupport() {
	}

	@Override
	public final String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "current value for " + sequenceName;
	}
}
