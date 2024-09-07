/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.common.TemporalUnit;

/**
 * A mini-"type system" for HQL function parameters.
 * <p>
 * Note that typical database type systems have relatively few types,
 * and lots of implicit type conversion between them. So we can be
 * pretty forgiving here.
 *
 * @author Gavin King
 *
 * @see ArgumentTypesValidator
 */
public enum FunctionParameterType {
	/**
	 * @see org.hibernate.type.SqlTypes#isCharacterType(int)
	 */
	STRING,
	/**
	 * @see org.hibernate.type.SqlTypes#isNumericType(int)
	 */
	NUMERIC,
	/**
	 * @see org.hibernate.type.SqlTypes#isIntegral(int)
	 */
	INTEGER,
	/**
	 * @see org.hibernate.type.SqlTypes#isTemporalType(int)
	 */
	TEMPORAL,
	/**
	 * @see org.hibernate.type.SqlTypes#hasDatePart(int)
	 */
	DATE,
	/**
	 * @see org.hibernate.type.SqlTypes#hasTimePart(int)
	 */
	TIME,
	/**
	 * Indicates that the argument should be of type
	 * {@link org.hibernate.type.SqlTypes#BOOLEAN} or
	 * a logical expression (predicate)
	 */
	BOOLEAN,
	/**
	 * Indicates a parameter that accepts any type
	 */
	ANY,
	/**
	 * A temporal unit, used by the {@code extract()} function, and
	 * some native database functions
	 *
	 * @see TemporalUnit
	 * @see org.hibernate.query.sqm.tree.expression.SqmExtractUnit
	 * @see org.hibernate.query.sqm.tree.expression.SqmDurationUnit
	 */
	TEMPORAL_UNIT,
	/**
	 * A trim specification, for trimming and padding functions
	 *
	 * @see org.hibernate.query.sqm.tree.expression.SqmTrimSpecification
	 */
	TRIM_SPEC,
	/**
	 * A collation, used by the {@code collate()} function
	 *
	 * @see org.hibernate.query.sqm.tree.expression.SqmCollation
	 */
	COLLATION,
	/**
	 * Any type with an order (numeric, string, and temporal types)
	 */
	COMPARABLE,
	/**
	 * @see org.hibernate.type.SqlTypes#isCharacterOrClobType(int)
	 */
	STRING_OR_CLOB,
	/**
	 * Indicates that the argument should be a spatial type
	 * @see org.hibernate.type.SqlTypes#isSpatialType(int) 
	 */
	SPATIAL,
	/**
	 * Indicates a parameter that accepts any type, except untyped expressions like {@code null} literals
	 */
	NO_UNTYPED
}
