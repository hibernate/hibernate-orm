/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An extendable concat() function definition which allows to cast arguments if necessary.
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class ConcatFunctionTemplate extends FunctionAsExpressionTemplate {

	public static final ConcatFunctionTemplate INSTANCE = new ConcatFunctionTemplate();

	public ConcatFunctionTemplate() {
		this( "(", "||", ")" );
	}

	public ConcatFunctionTemplate(String begin, String sep, String end) {
		super(
				begin,
				sep,
				end,
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.STRING ),
				StandardArgumentsValidators.min( 2 )
		);
	}
}
