/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.NamedSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.type.Type;

/**
 * Support for slightly more general templating than {@link NamedSqmFunctionTemplate}, with an unlimited number of arguments.
 *
 * @author Gavin King
 */
public class VarArgsSQLFunction implements SqmFunctionTemplate {
	private final String begin;
	private final String sep;
	private final String end;
	private final Type registeredType;

	/**
	 * Constructs a VarArgsSQLFunction instance with a 'static' return type.  An example of a 'static'
	 * return type would be something like an <tt>UPPER</tt> function which is always returning
	 * a SQL VARCHAR and thus a string type.
	 *
	 * @param registeredType The return type.
	 * @param begin The beginning of the function templating.
	 * @param sep The separator for each individual function argument.
	 * @param end The end of the function templating.
	 */
	public VarArgsSQLFunction(Type registeredType, String begin, String sep, String end) {
		this.registeredType = registeredType;
		this.begin = begin;
		this.sep = sep;
		this.end = end;
	}

	@Override
	public SqmExpression makeSqmFunctionExpression(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new SelfRenderingSqmFunction(
				(sqlAppender, sqlAstArguments, walker, sessionFactory) -> {
					sqlAppender.appendSql( begin );
					for ( Expression sqlAstArgument : sqlAstArguments ) {
						sqlAstArgument.accept( walker );
						sqlAppender.appendSql( ", " );
					}
					sqlAppender.appendSql( end );
				},
				arguments,
				impliedResultType
		);
	}

}
