/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.projection;

import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.EntityInstantiator;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface AuditProjection {

	/**
	 * Adds an audit projection to the specified query.
	 *
	 * @param enversService the Envers service
	 * @param auditReader the audit reader implementor
	 * @param aliasToEntityNameMap the entity name alias map
	 * @param baseAlias the base alias, if one is specified; may be {@literal null}
	 * @param queryBuilder the query builder
	 */
	void addProjectionToQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String baseAlias,
			QueryBuilder queryBuilder);

	/**
	 * Get the alias associated with the audit projection.
	 *
	 * @param baseAlias the base alias if one exists; may be {@literal null}
	 * @return the alias
	 */
	String getAlias(String baseAlias);

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
