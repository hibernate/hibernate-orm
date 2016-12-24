/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Models query returns and fetches.
 *
 * The actual returns from a query are defined by {@link org.hibernate.sql.convert.results.spi.Return} and its
 * sub-types.  Together with {@link org.hibernate.sql.convert.results.spi.Fetch} they define the structure
 * and shape of the SQL select clause and how to read back those results.
 */
package org.hibernate.sql.convert.results;
