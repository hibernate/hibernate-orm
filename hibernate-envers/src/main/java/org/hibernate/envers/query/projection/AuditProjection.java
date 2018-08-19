/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.projection;

import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditProjection {

	void addProjectionToQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String baseAlias,
			QueryBuilder queryBuilder);

	public String getAlias(String baseAlias);

	/**
	 * @param enversService the Envers service
	 * @param entityInstantiator the entity instantiator
	 * @param entityName the name of the entity for which the projection has been added
	 * @param revision the revision
	 * @param value the value to convert
	 * @return the converted value
	 */
	Object convertQueryResult(
			final EnversService enversService,
			final EntityInstantiator entityInstantiator,
			final String entityName,
			final Number revision,
			final Object value
	);

}
