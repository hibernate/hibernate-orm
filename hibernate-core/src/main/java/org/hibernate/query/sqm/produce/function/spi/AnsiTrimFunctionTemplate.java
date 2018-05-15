/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.spi.TrimSpecificationExpressionWrapper;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Defines support for rendering according to ANSI SQL <tt>TRIM</tt>
 * function specification.
 *
 * @author Steve Ebersole
 */
public class AnsiTrimFunctionTemplate implements SqmFunctionTemplate {

	/**
	 * A singleton instance for a function template that renders the "trim" function, as defined in ANSI SQL.
	 */
	public static final AnsiTrimFunctionTemplate INSTANCE = new AnsiTrimFunctionTemplate();

	private AnsiTrimFunctionTemplate() {
	}

	@Override
	public SqmFunction makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		// 1) LEADING/TRAILING/BOTH
		// 2) trim char
		// 3) source
		assert arguments.size() == 3;

		return new SqmTrimFunction(
				StandardSpiBasicTypes.STRING,
				( (TrimSpecificationExpressionWrapper) arguments.get( 0 ) ).getSpecification(),
				arguments.get( 1 ),
				arguments.get( 2 )
		);
	}
}
