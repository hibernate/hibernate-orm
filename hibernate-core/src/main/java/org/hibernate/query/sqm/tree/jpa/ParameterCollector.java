/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.jpa;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.query.sqm.consume.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.service.ServiceRegistry;

/**
 * todo (6.0) : how is this different from {@link org.hibernate.query.sqm.internal.ParameterCollector}?
 *
 * @author Steve Ebersole
 */
public class ParameterCollector extends BaseSemanticQueryWalker {
	public static Set<SqmParameter<?>> collectParameters(
			SqmStatement<?> statement,
			ServiceRegistry serviceRegistry) {
		final ParameterCollector collector = new ParameterCollector( serviceRegistry );
		statement.accept( collector );
		return collector.parameterExpressions == null
				? Collections.emptySet()
				: collector.parameterExpressions;
	}

	private ParameterCollector(ServiceRegistry serviceRegistry) {
		super( serviceRegistry );
	}

	private Set<SqmParameter<?>> parameterExpressions;

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return visitParameter( expression );
	}

	@Override
	public Object visitNamedParameterExpression(SqmNamedParameter expression) {
		return visitParameter( expression );
	}

	@Override
	public Object visitCriteriaParameter(SqmCriteriaParameter expression) {
		return visitParameter( expression );
	}

	private SqmParameter<?> visitParameter(SqmParameter<?> param) {
		if ( parameterExpressions == null ) {
			parameterExpressions = new HashSet<>();
		}

		parameterExpressions.add( param );

		return param;
	}

}
