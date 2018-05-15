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
 *
 * todo (6.0) : I think native-query QueryResult builders will need to work on a much different delayed paradigm...
 * 		the reason being the use of aliases by the user to "map" the JDBC results - I think this means we will
 * 		need the ResultSetMetadata to calculate names-to-positions.
 *
 * 		However, given that solution reading results back from the QueryResultCache can get tricky (strictly position based)
 * 		meaning we'd have to make sure that the "resolved" form of the resullt-mapping is kept.
 */
package org.hibernate.query.sql;
