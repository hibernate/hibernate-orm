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
 * An abstract class for all expression types which are atomic (i.e. expressions
 * which are not composed of one or more other expressions). For those expression
 * types which base class already calculates the effective alias and resolves
 * the corresponding entity name. The effect alias is either the alias that has been
 * specified at creation time of this expression or if that alias is null, the base
 * alias is used. This calculation is done in the
 * {@link AuditCriterion#addToQuery(EnversService, AuditReaderImplementor, Map, Map, String, QueryBuilder, Parameters)}
 * implementation and then delegated for the concrete work to the template method
 * {@link #addToQuery(EnversService, AuditReaderImplementor, String, String, String, QueryBuilder, Parameters)}
 *
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
abstract class AbstractAtomicExpression implements AuditCriterion {

	private final String alias;

	protected AbstractAtomicExpression(String alias) {
		this.alias = alias;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String baseAlias,
			QueryBuilder qb,
			Parameters parameters) {
		final String effectiveAlias = alias == null ? baseAlias : alias;
		final String entityName = aliasToEntityNameMap.get( effectiveAlias );
		final String componentPrefix = CriteriaTools.determineComponentPropertyPrefix(
				enversService,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				effectiveAlias
		);
		addToQuery(enversService, versionsReader, entityName, effectiveAlias, componentPrefix, qb, parameters);
	}

	protected abstract void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			String entityName,
			String alias,
			String componentPrefix,
			QueryBuilder qb,
			Parameters parameters);

}
