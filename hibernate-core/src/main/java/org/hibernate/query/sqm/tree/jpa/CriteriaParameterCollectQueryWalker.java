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
import java.util.function.Consumer;

import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;

/**
 * A criteria parameter collector to feed parameter collected to a {@link Consumer}
 * while walking a Semantic Query Model (SQM) tree.
 *
 * @author Steve Ebersole
 */
public class CriteriaParameterCollectQueryWalker extends BaseSemanticQueryWalker {

	public static Set<SqmParameter<?>> collectParameters(
			SqmStatement<?> statement,
			Consumer<SqmParameter<?>> consumer,
			ServiceRegistry serviceRegistry) {
		final CriteriaParameterCollectQueryWalker collector = new CriteriaParameterCollectQueryWalker( serviceRegistry, consumer );
		statement.accept( collector );
		return collector.parameterExpressions == null
				? Collections.emptySet()
				: collector.parameterExpressions;
	}

	private CriteriaParameterCollectQueryWalker(
			ServiceRegistry serviceRegistry,
			Consumer<SqmParameter<?>> consumer) {
		super( serviceRegistry );
		this.consumer = consumer;
	}

	private Set<SqmParameter<?>> parameterExpressions;
	private final Consumer<SqmParameter<?>> consumer;

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return visitParameter( expression );
	}

	@Override
	public Object visitNamedParameterExpression(SqmNamedParameter expression) {
		return visitParameter( expression );
	}

	/**
	 * This is called while performing an inflight parameter collection of parameters
	 * for `CriteriaQuery#getParameters`.  That method can be called multiple times and
	 * the parameters may have changed in between each call - therefore the parameters
	 * must be collected dynamically each time.
	 *
	 * This form simply returns the JpaCriteriaParameter
	 *
	 * @see SqmSelectStatement#resolveParameters()
	 */
	@Override
	public SqmJpaCriteriaParameterWrapper<?> visitJpaCriteriaParameter(JpaCriteriaParameter<?> expression) {
		//noinspection unchecked
		return (SqmJpaCriteriaParameterWrapper) visitParameter(
				new SqmJpaCriteriaParameterWrapper(
						expression.getHibernateType(),
						expression,
						expression.nodeBuilder()
				)
		);
	}

	private SqmParameter<?> visitParameter(SqmParameter<?> param) {
		if ( parameterExpressions == null ) {
			parameterExpressions = new HashSet<>();
		}

		parameterExpressions.add( param );

		consumer.accept( param );

		return param;
	}

}
