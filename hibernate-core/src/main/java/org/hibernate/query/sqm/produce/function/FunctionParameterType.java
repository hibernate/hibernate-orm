/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	 * @see org.hibernate.type.SqlTypes#isBinaryType(int)
	 */
	BINARY,
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
	 * Indicates that the argument should be a JSON type
	 * @see org.hibernate.type.SqlTypes#isJsonType(int)
	 * @since 7.0
	 */
	JSON,
	/**
	 * Indicates that the argument should be a JSON or String type
	 * @see org.hibernate.type.SqlTypes#isImplicitJsonType(int)
	 * @since 7.0
	 */
	IMPLICIT_JSON,
	/**
	 * Indicates that the argument should be an ENUM type
	 * @see org.hibernate.type.SqlTypes#isEnumType(int)
	 * @since 7.0
	 */
	ENUM,
	/**
	 * Indicates that the argument should be a XML type
	 * @see org.hibernate.type.SqlTypes#isXmlType(int)
	 * @since 7.0
	 */
	XML,
	/**
	 * Indicates that the argument should be a XML or String type
	 * @see org.hibernate.type.SqlTypes#isImplicitXmlType(int)
	 * @since 7.0
	 */
	IMPLICIT_XML,
	/**
	 * Indicates a parameter that accepts any type, except untyped expressions like {@code null} literals
	 */
	NO_UNTYPED
}
