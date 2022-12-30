/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Contains implementations of {@link org.hibernate.query.sqm.function.SqmFunctionDescriptor}
 * describing a range of relatively-sophisticated SQL functions available in various dialects.
 * <p>
 * The simplified implementations {@link org.hibernate.dialect.function.StandardSQLFunction} and
 * {@link org.hibernate.dialect.function.SQLFunctionTemplate} are provided to ease migration
 * from older version of Hibernate.
 */
package org.hibernate.dialect.function;
