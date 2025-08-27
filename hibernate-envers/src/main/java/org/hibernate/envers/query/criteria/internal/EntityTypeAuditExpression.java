/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria.internal;

import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class EntityTypeAuditExpression implements AuditCriterion {

	private String alias;
	private String entityName;

	public EntityTypeAuditExpression(
			String alias,
			String entityName) {
		this.alias = alias;
		this.entityName = entityName;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String baseAlias,
			QueryBuilder qb,
			Parameters parameters) {
		// todo: add contextual detail about query generation
		//
		// Take an example situation where a non-audited entity extends an audited-entity and uses the
		// AuditEntity#entityType method with the non-audited entity.  It would stand to reason that
		// it makes sense that we'd throw a NotAuditedException here rather than apply the restriction
		// anyway and return no results?
		//
		// Knowing whether EntityTypeAuditExpression is for an association traversal or part of the
		// entity inheritance criteria of the root entity would drive how we'd either throw an
		// exception or be lenient and permit adding the expression without validation.
		//
		// For now, we're just going to allow adding the criteria without any validation because the
		// code needs to support both traversal paths without any clear distinction.
		//
		String effectiveAlias = alias == null ? baseAlias : alias;
		String effectiveEntityName = entityName;
		if ( enversService.getEntitiesConfigurations().isVersioned( effectiveEntityName ) ) {
			effectiveEntityName = enversService.getConfig().getAuditEntityName( effectiveEntityName );
		}
		parameters.addEntityTypeRestriction( effectiveAlias, effectiveEntityName );
	}
}
