/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface AuditCriterion {
	/**
	 * @param enversService The EnversService
	 * @param versionsReader The AuditReader
	 * @param aliasToEntityNameMap the alias to entity name map
	 * @param baseAlias the base alias
	 * @param qb the query builder
	 * @param parameters the query parameters.
	 * @deprecated (since 6.0), use {@link #addToQuery(AuditReaderImplementor, Map, String, QueryBuilder, Parameters)}.
	 */
	@Deprecated
	default void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			String baseAlias,
			QueryBuilder qb,
			Parameters parameters) {
		addToQuery( versionsReader, aliasToEntityNameMap, baseAlias, qb, parameters );
	}

	void addToQuery(
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			String baseAlias,
			QueryBuilder qb,
			Parameters parameters);
}
