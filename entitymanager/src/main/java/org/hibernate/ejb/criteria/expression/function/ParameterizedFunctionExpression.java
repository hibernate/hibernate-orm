/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.Renderable;
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
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName,
			List<Expression<?>> argumentExpressions) {
		super( criteriaBuilder, javaType, functionName );
		this.argumentExpressions = argumentExpressions;
	}

	public ParameterizedFunctionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName,
			Expression<?>... argumentExpressions) {
		super( criteriaBuilder, javaType, functionName );
		this.argumentExpressions = Arrays.asList( argumentExpressions );
	}

	protected  static List<Expression<?>> wrapAsLiterals(CriteriaBuilderImpl criteriaBuilder, Object... literalArguments) {
		List<Expression<?>> arguments = new ArrayList<Expression<?>>( properSize( literalArguments.length) );
		for ( Object o : literalArguments ) {
			arguments.add( new LiteralExpression( criteriaBuilder, o ) );
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

	@Override
	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( getFunctionName() ).append( '(' );
		renderArguments( buffer, renderingContext );
		buffer.append( ')' );
		return buffer.toString();
	}

	protected void renderArguments(StringBuilder buffer, CriteriaQueryCompiler.RenderingContext renderingContext) {
		for ( Expression argument : argumentExpressions ) {
			buffer.append( ( (Renderable) argument ).render( renderingContext ) );
		}
	}
}
