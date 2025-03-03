/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * Defines the set of basic types which should be
 * accepted by the {@code cast()} function on every
 * platform.
 *
 * @implNote While almost every database supports
 * the ANSI {@code cast()} function, the actual type
 * conversions supported vary widely. Therefore, it
 * is sometimes necessary to emulate certain type
 * conversions that we consider "basic". In particular,
 * some databases (looking at you, MySQL and friends)
 * don't have a proper {@link java.sql.Types#BOOLEAN}
 * type, and so type conversions to and from
 * {@link Boolean} must be emulated.
 *
 * @apiNote This is an SPI type allowing collaboration
 * between {@code org.hibernate.dialect} and
 * {@code org.hibernate.sqm}. It should never occur in
 * APIs visible to the application program.
 *
 * @see org.hibernate.dialect.Dialect#castPattern(CastType, CastType)
 *
 * @author Gavin King
 */
public enum CastType {
	STRING, CLOB,
	BOOLEAN, INTEGER_BOOLEAN, YN_BOOLEAN, TF_BOOLEAN,
	INTEGER, LONG, FLOAT, DOUBLE, FIXED,
	DATE, TIME, TIMESTAMP,
	OFFSET_TIMESTAMP, ZONE_TIMESTAMP,
	JSON,
	XML,
	NULL,
	OTHER;

	public boolean isNumeric() {
		return switch ( this ) {
			case INTEGER, LONG, INTEGER_BOOLEAN, FLOAT, DOUBLE, FIXED -> true;
			default -> false;
		};
	}
}
