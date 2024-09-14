/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

/**
 * Defines the set of basic types which should be
 * accepted by the {@code cast()} function on every
 * platform.
 * <p>
 * Note that while almost every database supports
 * the ANSI {@code cast()} function, the actual type
 * conversions supported vary widely. Therefore, it
 * is sometimes necessary to emulate certain type
 * conversions that we consider "basic". In particular,
 * some databases (looking at you, MySQL and friends)
 * don't have a proper {@link java.sql.Types#BOOLEAN}
 * type, and so type conversions to and from
 * {@link Boolean} must be emulated.
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
	NULL,
	OTHER;

	public boolean isNumeric() {
		switch (this) {
			case INTEGER:
			case LONG:
			case INTEGER_BOOLEAN:
			case FLOAT:
			case DOUBLE:
			case FIXED:
				return true;
			default:
				return false;
		}
	}
}
