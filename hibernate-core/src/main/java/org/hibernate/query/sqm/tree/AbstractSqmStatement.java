/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import java.util.LinkedHashSet;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

import jakarta.persistence.criteria.ParameterExpression;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.query.sqm.tree.jpa.ParameterCollector.collectParameters;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmStatement<T> extends AbstractSqmNode implements SqmStatement<T>, ParameterCollector {
	private final SqmQuerySource querySource;
	private @Nullable Set<SqmParameter<?>> parameters;

	public AbstractSqmStatement(
			SqmQuerySource querySource,
			NodeBuilder builder) {
		super( builder );
		this.querySource = querySource;
	}

	protected AbstractSqmStatement(
			NodeBuilder builder,
			SqmQuerySource querySource,
			@Nullable Set<SqmParameter<?>> parameters) {
		super( builder );
		this.querySource = querySource;
		this.parameters = parameters;
	}

	protected @Nullable Set<SqmParameter<?>> copyParameters(SqmCopyContext context) {
		if ( parameters == null ) {
			return null;
		}
		else {
			final Set<SqmParameter<?>> parameters = new LinkedHashSet<>( this.parameters.size() );
			for ( var parameter : this.parameters ) {
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
			parameters = new LinkedHashSet<>();
		}
		parameters.add( parameter );
	}

	@Override
	public Set<SqmParameter<?>> getSqmParameters() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			assert parameters == null : "SqmSelectStatement (as Criteria) should not have collected parameters";
			return collectParameters( this );
		}
		else {
			return parameters == null ? emptySet() : unmodifiableSet( parameters );
		}
	}

	@Override
	public ParameterResolutions resolveParameters() {
		return SqmUtil.resolveParameters( this );
	}

	@Override
	public Set<ParameterExpression<?>> getParameters() {
		// At this level, the number of parameters may still be growing as
		// nodes are added to the Criteria, so we recalculate this every time.
		// For a finalized set of parameters, use resolveParameters() instead
		assert querySource == SqmQuerySource.CRITERIA;
		return SqmUtil.getParameters( this );
	}

	private int aliasCounter = 0;

	@Override
	public String generateAlias() {
		return "var_" + (++aliasCounter);
	}
}
