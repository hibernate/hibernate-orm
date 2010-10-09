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
package org.hibernate.ejb.criteria.predicate;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.ValueHandlerFactory;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.Renderable;
import org.hibernate.ejb.criteria.expression.BinaryOperatorExpression;
import org.hibernate.ejb.criteria.expression.LiteralExpression;

/**
 * Models a basic relational comparison predicate.
 *
 * @author Steve Ebersole
 */
public class ComparisonPredicate
		extends AbstractSimplePredicate
		implements BinaryOperatorExpression<Boolean>, Serializable {
	private final ComparisonOperator comparisonOperator;
	private final Expression<?> leftHandSide;
	private final Expression<?> rightHandSide;

	public ComparisonPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			ComparisonOperator comparisonOperator,
			Expression<?> leftHandSide,
			Expression<?> rightHandSide) {
		super( criteriaBuilder );
		this.comparisonOperator = comparisonOperator;
		this.leftHandSide = leftHandSide;
		this.rightHandSide = rightHandSide;
	}

	@SuppressWarnings({ "unchecked" })
	public ComparisonPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			ComparisonOperator comparisonOperator,
			Expression<?> leftHandSide,
			Object rightHandSide) {
		super( criteriaBuilder );
		this.comparisonOperator = comparisonOperator;
		this.leftHandSide = leftHandSide;
		if ( ValueHandlerFactory.isNumeric( leftHandSide.getJavaType() ) ) {
			this.rightHandSide = new LiteralExpression(
					criteriaBuilder,
					ValueHandlerFactory.convert( rightHandSide, (Class<Number>) leftHandSide.getJavaType() )
			);
		}
		else {
			this.rightHandSide = new LiteralExpression( criteriaBuilder, rightHandSide );
		}
	}

	public <N extends Number> ComparisonPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			ComparisonOperator comparisonOperator,
			Expression<N> leftHandSide,
			Number rightHandSide) {
		super( criteriaBuilder );
		this.comparisonOperator = comparisonOperator;
		this.leftHandSide = leftHandSide;
		this.rightHandSide = new LiteralExpression<N>( 
				criteriaBuilder,
				ValueHandlerFactory.convert( rightHandSide, leftHandSide.getJavaType() )
		);
	}

	public ComparisonOperator getComparisonOperator() {
		return isNegated()
				? comparisonOperator.negated()
				: comparisonOperator;
	}

	public Expression getLeftHandOperand() {
		return leftHandSide;
	}

	public Expression getRightHandOperand() {
		return rightHandSide;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getLeftHandOperand(), registry );
		Helper.possibleParameter( getRightHandOperand(), registry );
	}

	/**
	 * Defines the comparison operators.  We could also get away with
	 * only 3 and use negation...
	 */
	public static enum ComparisonOperator {
		EQUAL {
			public ComparisonOperator negated() {
				return NOT_EQUAL;
			}
			public String rendered() {
				return "=";
			}
		},
		NOT_EQUAL {
			public ComparisonOperator negated() {
				return EQUAL;
			}
			public String rendered() {
				return "<>";
			}
		},
		LESS_THAN {
			public ComparisonOperator negated() {
				return GREATER_THAN_OR_EQUAL;
			}
			public String rendered() {
				return "<";
			}
		},
		LESS_THAN_OR_EQUAL {
			public ComparisonOperator negated() {
				return GREATER_THAN;
			}
			public String rendered() {
				return "<=";
			}
		},
		GREATER_THAN {
			public ComparisonOperator negated() {
				return LESS_THAN_OR_EQUAL;
			}
			public String rendered() {
				return ">";
			}
		},
		GREATER_THAN_OR_EQUAL {
			public ComparisonOperator negated() {
				return LESS_THAN;
			}
			public String rendered() {
				return ">=";
			}
		};

		public abstract ComparisonOperator negated();

		public abstract String rendered();
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return ( (Renderable) getLeftHandOperand() ).render( renderingContext )
				+ getComparisonOperator().rendered()
				+ ( (Renderable) getRightHandOperand() ).render( renderingContext );
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
