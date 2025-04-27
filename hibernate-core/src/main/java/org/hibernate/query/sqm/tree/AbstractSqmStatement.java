/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.ValueBindJpaCriteriaParameter;

import jakarta.persistence.criteria.ParameterExpression;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.query.sqm.tree.jpa.ParameterCollector.collectParameters;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmStatement<T> extends AbstractSqmNode implements SqmStatement<T>, ParameterCollector {
	private final SqmQuerySource querySource;
	private Set<SqmParameter<?>> parameters;

	public AbstractSqmStatement(
			SqmQuerySource querySource,
			NodeBuilder builder) {
		super( builder );
		this.querySource = querySource;
	}

	protected AbstractSqmStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			Set<SqmParameter<?>> parameters) {
		super( builder );
		this.querySource = querySource;
		this.parameters = parameters;
	}

	protected Set<SqmParameter<?>> copyParameters(SqmCopyContext context) {
		if ( parameters == null ) {
			return null;
		}
		else {
			final Set<SqmParameter<?>> parameters = new HashSet<>( this.parameters.size() );
			for ( SqmParameter<?> parameter : this.parameters ) {
				parameters.add( parameter.copy( context ) );
			}
			return parameters;
		}
	}

	@Override
	public SqmQuerySource getQuerySource() {
		return querySource;
	}

	@Override
	public void addParameter(SqmParameter<?> parameter) {
		if ( parameters == null ) {
			parameters = new HashSet<>();
		}

		parameters.add( parameter );
	}

	@Override
	public Set<SqmParameter<?>> getSqmParameters() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			assert parameters == null : "SqmSelectStatement (as Criteria) should not have collected parameters";
			return collectParameters( this );
		}

		return parameters == null ? emptySet() : unmodifiableSet( parameters );
	}

	@Override
	public ParameterResolutions resolveParameters() {
		return SqmUtil.resolveParameters( this );
	}

	@Override
	public Set<ParameterExpression<?>> getParameters() {
		// At this level, the number of parameters may still be growing as
		// nodes are added to the Criteria - so we re-calculate this every
		// time.
		//
		// for a "finalized" set of parameters, use `#resolveParameters` instead
		assert getQuerySource() == SqmQuerySource.CRITERIA;
		return getSqmParameters().stream()
				.filter( parameterExpression -> !( parameterExpression instanceof ValueBindJpaCriteriaParameter ) )
				.collect( Collectors.toSet() );
	}

	private int aliasCounter = 0;

	@Override
	public String generateAlias() {
		return "_" + (++aliasCounter);
	}
}
