/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.criteria.predicate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterContainer;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.expression.ExpressionImpl;

/**
 * @author Steve Ebersole
 */
public class NegatedPredicateWrapper extends ExpressionImpl<Boolean> implements PredicateImplementor, Serializable {
	private final PredicateImplementor predicate;
	private final BooleanOperator negatedOperator;
	private final List<Expression<Boolean>> negatedExpressions;

	@SuppressWarnings("unchecked")
	public NegatedPredicateWrapper(PredicateImplementor predicate) {
		super( predicate.criteriaBuilder(), Boolean.class );
		this.predicate = predicate;
		this.negatedOperator = predicate.isJunction()
				? CompoundPredicate.reverseOperator( predicate.getOperator() )
				: predicate.getOperator();
		this.negatedExpressions = negateCompoundExpressions( predicate.getExpressions(), predicate.criteriaBuilder() );
	}

	private static List<Expression<Boolean>> negateCompoundExpressions(
			List<Expression<Boolean>> expressions,
			CriteriaBuilderImpl criteriaBuilder) {
		if ( expressions == null || expressions.isEmpty() ) {
			return Collections.emptyList();
		}

		final List<Expression<Boolean>> negatedExpressions = new ArrayList<Expression<Boolean>>();
		for ( Expression<Boolean> expression : expressions ) {
			if ( Predicate.class.isInstance( expression ) ) {
				negatedExpressions.add( ( (Predicate) expression ).not() );
			}
			else {
				negatedExpressions.add( criteriaBuilder.not( expression ) );
			}
		}
		return negatedExpressions;
	}

	@Override
	public BooleanOperator getOperator() {
		return negatedOperator;
	}

	@Override
	public boolean isJunction() {
		return predicate.isJunction();
	}

	@Override
	public boolean isNegated() {
		return ! predicate.isNegated();
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return negatedExpressions;
	}

	@Override
	public Predicate not() {
		return new NegatedPredicateWrapper( this );
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		if ( ParameterContainer.class.isInstance( predicate ) ) {
			( (ParameterContainer) predicate ).registerParameters( registry );
		}
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		if ( isJunction() ) {
			return CompoundPredicate.render( this, renderingContext );
		}
		else {
			return predicate.render( isNegated, renderingContext );
		}
	}

	@Override
	public String render(RenderingContext renderingContext) {
		return render( isNegated(), renderingContext );
	}

	@Override
	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
