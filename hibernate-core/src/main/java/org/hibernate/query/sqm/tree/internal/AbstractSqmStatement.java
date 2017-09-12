/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.SemanticException;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmStatement implements SqmStatement, ParameterCollector {
	private Map<String,SqmNamedParameter> namedQueryParameters;
	private Map<Integer,SqmPositionalParameter> positionalQueryParameters;

	@Override
	public void addParameter(SqmNamedParameter parameter) {
		assert parameter.getName() != null;
		assert parameter.getPosition() == null;

		if ( namedQueryParameters == null ) {
			namedQueryParameters = new ConcurrentHashMap<>();
		}

		namedQueryParameters.put( parameter.getName(), parameter );
	}

	@Override
	public void addParameter(SqmPositionalParameter parameter) {
		assert parameter.getPosition() != null;
		assert parameter.getName() == null;

		if ( positionalQueryParameters == null ) {
			positionalQueryParameters = new ConcurrentHashMap<>();
		}

		positionalQueryParameters.put( parameter.getPosition(), parameter );
	}

	public void wrapUp() {
		validateParameters();
	}

	private void validateParameters() {
		if ( positionalQueryParameters == null ) {
			return;
		}

		// validate the positions.  JPA says that these should start with 1 and
		// increment contiguously (no gaps)
		int[] positionsArray = positionalQueryParameters.keySet().stream().mapToInt( Integer::intValue ).toArray();
		Arrays.sort( positionsArray );

		int previous = 0;
		for ( Integer position : positionsArray ) {
			if ( position != previous + 1 ) {
				if ( previous == 0 ) {
					throw new SemanticException( "Positional parameters did not start with 1 : " + position );
				}
				else {
					throw new SemanticException( "Gap in positional parameter positions; skipped " + (previous+1) );
				}
			}
			previous = position;
		}
	}

	@Override
	public Set<SqmParameter> getQueryParameters() {
		Set<SqmParameter> parameters = new HashSet<>();
		if ( namedQueryParameters != null ) {
			parameters.addAll( namedQueryParameters.values() );
		}
		if ( positionalQueryParameters != null ) {
			parameters.addAll( positionalQueryParameters.values() );
		}
		return parameters;
	}
}
