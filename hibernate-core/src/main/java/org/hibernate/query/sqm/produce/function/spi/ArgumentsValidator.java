/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;
import java.util.Locale;

import org.hibernate.query.sqm.QueryException;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface ArgumentsValidator {
	/**
	 * The main (functional) operation defining validation
	 */
	void validate(List<SqmExpression> arguments);

	/**
	 * Static validator for performing no validation
	 */
	ArgumentsValidator NONE = arguments -> {};

	/**
	 * Static validator for verifying that we have no arguments
	 */
	ArgumentsValidator NO_ARGS = arguments -> {
		if ( !arguments.isEmpty() ) {
			throw new QueryException( "Expecting no arguments, but found " + arguments.size() );
		}
	};

}
