/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			String baseAlias,
			QueryBuilder qb,
			Parameters parameters) {
		String effectiveAlias = alias == null ? baseAlias : alias;
		String effectiveEntityName = entityName;
		if ( enversService.getEntitiesConfigurations().isVersioned( effectiveEntityName ) ) {
			effectiveEntityName = enversService.getAuditEntitiesConfiguration().getAuditEntityName( effectiveEntityName );
		}
		parameters.addEntityTypeRestriction( effectiveAlias, effectiveEntityName );
	}
}
