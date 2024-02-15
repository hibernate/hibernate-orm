/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.hql.spi.SqmCreationOptions;
import org.hibernate.query.spi.QueryEngineOptions;

/**
 * @author Steve Ebersole
 */
public class SqmCreationOptionsStandard implements SqmCreationOptions {
	private final QueryEngineOptions queryEngineOptions;

	public SqmCreationOptionsStandard(QueryEngineOptions queryEngineOptions) {
		this.queryEngineOptions = queryEngineOptions;
	}

	@Override
	public boolean useStrictJpaCompliance() {
		return queryEngineOptions.getJpaCompliance().isJpaQueryComplianceEnabled();
	}

	@Override
	public boolean isPortableIntegerDivisionEnabled() {
		return queryEngineOptions.isPortableIntegerDivisionEnabled();
	}
}
