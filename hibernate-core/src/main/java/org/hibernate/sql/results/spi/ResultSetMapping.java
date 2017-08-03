/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.List;

/**
 * The "resolved" form of {@link ResultSetMappingDescriptor} providing access to JDBC
 * results ({@link SqlSelection}) and object results ({@link QueryResult}).
 *
 * @see ResultSetMappingDescriptor#resolve
 *
 * @author Steve Ebersole
 */
public interface ResultSetMapping {
	List<SqlSelection> getSqlSelections();
	List<QueryResult> getQueryResults();
}
