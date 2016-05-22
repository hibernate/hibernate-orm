/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class AuditConjunction implements AuditCriterion, ExtendableCriterion {
	private List<AuditCriterion> criterions;

	public AuditConjunction() {
		criterions = new ArrayList<>();
	}

	@Override
	public AuditConjunction add(AuditCriterion criterion) {
		criterions.add( criterion );
		return this;
	}

	@Override
	public void addToQuery(
			EnversService enversService,
			AuditReaderImplementor versionsReader,
			Map<String, String> aliasToEntityNameMap,
			String alias,
			QueryBuilder qb,
			Parameters parameters) {
		Parameters andParameters = parameters.addSubParameters( Parameters.AND );

		if ( criterions.size() == 0 ) {
			andParameters.addWhere( "1", false, "=", "1", false );
		}
		else {
			for ( AuditCriterion criterion : criterions ) {
				criterion.addToQuery( enversService, versionsReader, aliasToEntityNameMap, alias, qb, andParameters );
			}
		}
	}
}
