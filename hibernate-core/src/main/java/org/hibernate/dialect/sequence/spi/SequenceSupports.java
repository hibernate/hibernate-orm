/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.sequence.internal.NoSequenceSupport;

import static org.hibernate.SPI.Role.USE;

/// Provides stable stock sequence-support strategies without exposing
/// Hibernate's vendor-specific implementations.
///
/// Use [#none()] for a database without sequence support. Use [#ansi()],
/// [#nextval()], or [#db2()] only when the corresponding complete grammar
/// matches the database. Providers with different behavior should implement
/// [SequenceSupport] or extend one of the supported family bases.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class SequenceSupports {
	private static final SequenceSupport NONE = new NoSequenceSupport();
	private static final SequenceSupport ANSI = new ANSISequenceSupport();
	private static final SequenceSupport NEXTVAL = new NextvalSequenceSupport();
	private static final SequenceSupport DB2 = new DB2SequenceSupport();

	private SequenceSupports() {
	}

	/// Return the stable strategy which rejects every sequence operation.
	public static SequenceSupport none() {
		return NONE;
	}

	/// Return the stable ANSI `next value for` strategy.
	public static SequenceSupport ansi() {
		return ANSI;
	}

	/// Return the stable `sequence.nextval` strategy.
	public static SequenceSupport nextval() {
		return NEXTVAL;
	}

	/// Return the stable Db2/Derby family strategy.
	public static SequenceSupport db2() {
		return DB2;
	}
}
