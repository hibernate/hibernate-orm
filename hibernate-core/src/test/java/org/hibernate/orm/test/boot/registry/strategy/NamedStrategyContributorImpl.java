/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.registry.strategy;

import org.hibernate.boot.registry.selector.spi.NamedStrategyContributions;
import org.hibernate.boot.registry.selector.spi.NamedStrategyContributor;

/**
 * @author Steve Ebersole
 */
public class NamedStrategyContributorImpl implements NamedStrategyContributor {
	@Override
	public void contributeStrategyImplementations(NamedStrategyContributions contributions) {
		contributions.contributeStrategyImplementor(
				StrategyContract.class,
				StrategyContractImpl.class,
				StrategyContractImpl.class.getName(),
				StrategyContractImpl.class.getSimpleName(),
				"main",
				"alternate"
		);
	}

	@Override
	public void clearStrategyImplementations(NamedStrategyContributions contributions) {
		contributions.removeStrategyImplementor( StrategyContract.class, StrategyContractImpl.class );
	}
}
