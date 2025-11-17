/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class AuditDisjunction implements AuditCriterion, ExtendableCriterion {
	private List<AuditCriterion> criterions;

	public AuditDisjunction() {
		criterions = new ArrayList<>();
	}

	@Override
	public AuditDisjunction add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			Map<String, String> aliasToComponentPropertyNameMap,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		Parameters orParameters = parameters.addSubParameters( Parameters.OR );

		if ( criterions.size() == 0 ) {
			orParameters.addWhere( "0", false, "=", "1", false );
		}
		else {
			for ( AuditCriterion criterion : criterions ) {
				criterion.addToQuery(
						enversService,
						versionsReader,
						aliasToEntityNameMap,
						aliasToComponentPropertyNameMap,
						alias,
						qb,
						orParameters
				);
			}
		}
	}
}
