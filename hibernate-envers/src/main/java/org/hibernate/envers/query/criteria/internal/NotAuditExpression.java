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
 * @author Adam Warski (adam at warski dot org)
 */
public class NotAuditExpression implements AuditCriterion {
	private AuditCriterion criterion;

	public NotAuditExpression(AuditCriterion criterion) {
		this.criterion = criterion;
	}

	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		criterion.addToQuery(
				enversService,
				versionsReader,
				aliasToEntityNameMap,
				aliasToComponentPropertyNameMap,
				alias,
				qb,
				parameters.addNegatedParameters()
		);
	}
}
