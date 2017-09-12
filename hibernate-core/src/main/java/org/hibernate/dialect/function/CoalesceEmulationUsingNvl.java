/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Emulation `coalesce` using `nvl` on versions Oracle not supporting `coalesce`
 *
 * todo (6.0) : what was the first version of Oracle to support `coalesce`?
 *
 * @author Steve Ebersole
 */
public class CoalesceEmulationUsingNvl
		extends AbstractSqmFunctionTemplate {
	/**
	 * Singleton access
	 */
	public static final CoalesceEmulationUsingNvl INSTANCE = new CoalesceEmulationUsingNvl();

	public CoalesceEmulationUsingNvl() {
		super( StandardArgumentsValidators.min( 2 ) );
	}

	@Override
	protected SqmExpression generateSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		SqmExpression nvl = nvl(
				arguments.get( arguments.size() - 1 ),
				arguments.get( arguments.size() - 2 )
		);

		for ( int i = arguments.size() - 3; i >= 0 ; i-- ) {
			nvl = nvl( arguments.get( i ), nvl );
		}

		return nvl;
	}

	protected SqmExpression nvl(SqmExpression arg1, SqmExpression arg2) {
		return new NvlFunctionTemplate.SqmNvlFunction(
				arg1,
				arg2,
				(AllowableFunctionReturnType) (arg1.getExpressableType() == null
						? arg2.getExpressableType()
						: arg1.getExpressableType())
		);
	}
}
