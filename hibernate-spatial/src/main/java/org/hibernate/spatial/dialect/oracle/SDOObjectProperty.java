/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Special function for accessing a member variable of an Oracle Object
 *
 * @author Karel Maesen
 */
class SDOObjectProperty implements SqmFunctionDescriptor {

	private final BasicTypeReference<?> type;

	private final String name;

	public SDOObjectProperty(String name, BasicTypeReference<?> type) {
		this.type = type;
		this.name = name;
	}

	public BasicTypeReference<?> getReturnType(BasicTypeReference<?> columnType, Mapping mapping)
			throws QueryException {
		return type == null ? columnType : type;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.dialect.function.SQLFunction#hasArguments()
	 */

	public boolean hasArguments() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.dialect.function.SQLFunction#hasParenthesesIfNoArguments()
	 */

	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.hibernate.dialect.function.SQLFunction#render(java.util.List,
	 *      org.hibernate.engine.SessionFactoryImplementor)
	 */

	public String render(Type firstArgtype, List args, SessionFactoryImplementor factory)
			throws QueryException {
		final StringBuilder buf = new StringBuilder();
		if ( args.isEmpty() ) {
			throw new QueryException(
					"First Argument in arglist must be object of which property is queried"
			);
		}
		buf.append( args.get( 0 ) ).append( "." ).append( name );
		return buf.toString();
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		throw new NotYetImplementedException();
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateAggregateSqmExpression(
			List<? extends SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return SqmFunctionDescriptor.super.generateAggregateSqmExpression(
				arguments,
				filter,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			SqmTypedNode<?> argument,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return SqmFunctionDescriptor.super.generateSqmExpression(
				argument,
				impliedResultType,
				queryEngine,
				typeConfiguration
		);
	}

	@Override
	public <T> SelfRenderingSqmFunction<T> generateSqmExpression(
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return SqmFunctionDescriptor.super.generateSqmExpression( impliedResultType, queryEngine, typeConfiguration );
	}

	@Override
	public boolean alwaysIncludesParentheses() {
		return SqmFunctionDescriptor.super.alwaysIncludesParentheses();
	}

	@Override
	public String getSignature(String name) {
		return SqmFunctionDescriptor.super.getSignature( name );
	}
}
