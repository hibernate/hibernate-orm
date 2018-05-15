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
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;

/**
 * ANSI-SQL style {@code cast(foo as type)} where the type is a Hibernate type
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CastFunctionTemplate implements SqmFunctionTemplate {
	@Override
	public SqmFunction makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		assert arguments.size() == 1;
		return new SqmCastFunction(
				arguments.get( 0 ),
				impliedResultType
		);
	}
}
