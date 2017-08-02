/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for "native SQL" queries.
 *
 * The contracts here try to stay as close as possible to the JPA
 * counterparts, but diverge in cases where Hibernate offers additional
 * features.
 *
 * NOTE: Named `sql` here rather than the preferred `native` since the latter
 * is a Java keyword.
 *
 * @see org.hibernate.query.NativeQuery
 */
package org.hibernate.query.sql;
