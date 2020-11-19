/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.SqmExpressable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;

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
 * see org.hibernate.dialect.Dialect#castPattern(CastType, CastType)
 *
 * @author Gavin King
 */
public enum CastType {
	STRING(CastTypeKind.TEXT),
	BOOLEAN(CastTypeKind.BOOLEAN),
	INTEGER(CastTypeKind.NUMERIC), LONG(CastTypeKind.NUMERIC), FLOAT(CastTypeKind.NUMERIC), DOUBLE(CastTypeKind.NUMERIC), FIXED(CastTypeKind.NUMERIC),
	DATE(CastTypeKind.TEMPORAL), TIME(CastTypeKind.TEMPORAL), TIMESTAMP(CastTypeKind.TEMPORAL),
	OFFSET_TIMESTAMP(CastTypeKind.TEMPORAL), ZONE_TIMESTAMP(CastTypeKind.TEMPORAL),
	NULL(null),
	OTHER(null);

	private final CastTypeKind kind;

	CastType(CastTypeKind kind) {
		this.kind = kind;
	}

	public CastTypeKind getKind() {
		return kind;
	}

	public static CastType from(Class javaClass) {
		if (String.class.equals(javaClass)) {
			return STRING;
		}
		if (Boolean.class.equals(javaClass)
				|| boolean.class.equals(javaClass)) {
			return BOOLEAN;
		}
		if (Integer.class.equals(javaClass)
				|| int.class.equals(javaClass)) {
			return INTEGER;
		}
		if (Long.class.equals(javaClass)
				|| long.class.equals(javaClass)) {
			return LONG;
		}
		if (Float.class.equals(javaClass)
				|| float.class.equals(javaClass)) {
			return FLOAT;
		}
		if (Double.class.equals(javaClass)
				|| double.class.equals(javaClass)) {
			return DOUBLE;
		}
		if (BigInteger.class.equals(javaClass)) {
			return FIXED;
		}
		if (BigDecimal.class.equals(javaClass)) {
			return FIXED;
		}
		if (LocalDate.class.equals(javaClass)) {
			return DATE;
		}
		if (LocalTime.class.equals(javaClass)) {
			return TIME;
		}
		if (LocalDateTime.class.equals(javaClass)) {
			return TIMESTAMP;
		}
		if (OffsetDateTime.class.equals(javaClass)) {
			return OFFSET_TIMESTAMP;
		}
		if (ZonedDateTime.class.equals(javaClass)) {
			return ZONE_TIMESTAMP;
		}
		return OTHER;
	}

	public static CastType from(JdbcMapping jdbcMapping) {
		//TODO: I really don't like using the Java type
		//      here. Is there some way to base this
		//      off of the mapped SQL type?
		return jdbcMapping == null ? NULL : from( jdbcMapping.getJavaTypeDescriptor().getJavaType() );
	}
}
