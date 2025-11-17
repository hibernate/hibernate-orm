/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	public boolean isJsonFunctionsEnabled() {
		return queryEngineOptions.isJsonFunctionsEnabled();
	}

	@Override
	public boolean isXmlFunctionsEnabled() {
		return queryEngineOptions.isXmlFunctionsEnabled();
	}

	@Override
	public boolean isPortableIntegerDivisionEnabled() {
		return queryEngineOptions.isPortableIntegerDivisionEnabled();
	}
}
