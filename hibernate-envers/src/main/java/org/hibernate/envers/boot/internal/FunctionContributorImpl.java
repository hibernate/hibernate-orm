/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.envers.function.OrderByFragmentFunction;

/**
 * Envers specific FunctionContributor
 *
 * @author Christian Beikov
 */
public class FunctionContributorImpl implements FunctionContributor {

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		final EnversService enversService = functionContributions.getServiceRegistry().getService( EnversService.class );
		if ( !enversService.isEnabled() ) {
			return;
		}

		functionContributions.getFunctionRegistry().register( OrderByFragmentFunction.FUNCTION_NAME, OrderByFragmentFunction.INSTANCE );
	}

}
