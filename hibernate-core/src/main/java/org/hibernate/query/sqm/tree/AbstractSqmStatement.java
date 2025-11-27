/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

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
			final Set<SqmParameter<?>> parameters = new LinkedHashSet<>( this.parameters.size() );
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

		return parameters == null ? Collections.emptySet() : Collections.unmodifiableSet( parameters );
	}

	@Override
	public ParameterResolutions resolveParameters() {
		return SqmUtil.resolveParameters( this );
	}
}
