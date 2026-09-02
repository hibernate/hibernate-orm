/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Standard family base for databases using `sequence.nextval` and
/// `sequence.currval` expressions.
///
/// Extend this class only when both expression forms match the database, and
/// override the surrounding select or DDL grammar where necessary.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT })
public class NextvalSequenceSupport implements SequenceSupport {
	/// Create a nextval-family strategy or provider subclass.
	@SPI(IMPLEMENT)
	public NextvalSequenceSupport() {
	}

	@Override
	public final String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public final String getSelectSequencePreviousValString(String sequenceName) {
		return sequenceName + ".currval";
	}

}
