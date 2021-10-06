/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.envers.function.OrderByFragmentFunction;
import org.hibernate.envers.internal.entities.RevisionTypeType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;

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
