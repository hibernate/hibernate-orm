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
import org.hibernate.envers.query.criteria.AuditFunction;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class PropertyFunctionAuditExpression implements AuditCriterion {

	private AuditFunction function;
	private String otherAlias;
	private String otherPropertyName;
	private String op;

	public PropertyFunctionAuditExpression(
			AuditFunction function,
			String otherAlias,
			String otherPropertyName,
			String op) {
		this.function = function;
		this.otherAlias = otherAlias;
		this.otherPropertyName = otherPropertyName;
		this.op = op;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor auditReader,
			Map<String, String> aliasToEntityNameMap,
			String baseAlias,
			QueryBuilder queryBuilder,
			Parameters parameters) {
		String effectiveOtherAlias = otherAlias == null ? baseAlias : otherAlias;
		String otherEntityName = aliasToEntityNameMap.get( effectiveOtherAlias );
		/*
		 * Check that the other property name is not a relation. However, we can only do this for audited entities. If
		 * the other property belongs to a non-audited entity, we have to skip this check.
		 */
		if ( enversService.getEntitiesConfigurations().isVersioned( otherEntityName ) ) {
			CriteriaTools.checkPropertyNotARelation( enversService, otherEntityName, otherPropertyName );
		}
		parameters.addWhereWithFunction( enversService, aliasToEntityNameMap, function, op, effectiveOtherAlias, otherPropertyName );
	}
}
