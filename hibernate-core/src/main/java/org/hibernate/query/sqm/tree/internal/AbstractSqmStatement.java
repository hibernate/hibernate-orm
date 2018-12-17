/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmStatement implements SqmStatement, ParameterCollector {
	private Set<SqmParameter> parameters;

	@Override
	public void addParameter(SqmParameter parameter) {
		if ( parameters == null ) {
			parameters = new HashSet<>();
		}

		parameters.add( parameter );
	}

	public void wrapUp() {
	}


	@Override
	public Set<SqmParameter> getQueryParameters() {
		return parameters;
	}
}
