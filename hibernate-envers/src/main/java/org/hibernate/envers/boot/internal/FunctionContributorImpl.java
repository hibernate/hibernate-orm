/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

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
	public void contributeFunctions(SqmFunctionRegistry functionRegistry, ServiceRegistry serviceRegistry) {
		final EnversService enversService = serviceRegistry.getService( EnversService.class );
		if ( !enversService.isEnabled() ) {
			return;
		}

		functionRegistry.register( OrderByFragmentFunction.FUNCTION_NAME, OrderByFragmentFunction.INSTANCE );
	}

}
