/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * Marker for all object types that can be part of a ResultSetMapping.
 *
 * Both {@link QueryResult} and {@link Fetch} are ResultSetMappingNode sub-types.
 *
 * Additionally ResultSetMappingNode is classified into
 *
 * @author Steve Ebersole
 */
public interface ResultSetMappingNode {
}
