/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria.expression.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.ParameterContainer;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * Support for functions with parameters.
 *
 * @author Steve Ebersole
 */
public class ParameterizedFunctionExpression<X>
		extends BasicFunctionExpression<X>
		implements FunctionExpression<X> {

	private final List<Expression<?>> argumentExpressions;

	public ParameterizedFunctionExpression(
			QueryBuilderImpl queryBuilder,
			Class<X> javaType,
			String functionName,
			List<Expression<?>> argumentExpressions) {
		super( queryBuilder, javaType, functionName );
		this.argumentExpressions = argumentExpressions;
	}

	public ParameterizedFunctionExpression(
			QueryBuilderImpl queryBuilder,
			Class<X> javaType,
			String functionName,
			Expression<?>... argumentExpressions) {
		super( queryBuilder, javaType, functionName );
		this.argumentExpressions = Arrays.asList( argumentExpressions );
	}

	protected  static List<Expression<?>> wrapAsLiterals(QueryBuilderImpl queryBuilder, Object... literalArguments) {
		List<Expression<?>> arguments = new ArrayList<Expression<?>>( properSize( literalArguments.length) );
		for ( Object o : literalArguments ) {
			arguments.add( new LiteralExpression( queryBuilder, o ) );
		}
		return arguments;
	}

	protected  static int properSize(int number) {
		return number + (int)( number*.75 ) + 1;
	}

	public List<Expression<?>> getArgumentExpressions() {
		return argumentExpressions;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		for ( Expression argument : getArgumentExpressions() ) {
			if ( ParameterContainer.class.isInstance( argument ) ) {
				( (ParameterContainer) argument ).registerParameters(registry);
			}
		}
	}

}
