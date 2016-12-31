/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.sqm.parser.criteria.tree.CriteriaVisitor;
import org.hibernate.sqm.query.predicate.SqmPredicate;

/**
 * Represents a grouping of JPA {@link Predicate} nodes as either a
 * conjunction (logical AND) or a disjunction (logical OR).
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public class JpaCompoundPredicate
		extends AbstractPredicateImpl
		implements JpaPredicateImplementor {
	private BooleanOperator operator;
	private final List<JpaPredicateImplementor> groupedPredicates = new ArrayList<>();

	/**
	 * Constructs an empty conjunction or disjunction.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 */
	public JpaCompoundPredicate(
			HibernateCriteriaBuilder criteriaBuilder,
			BooleanOperator operator) {
		super( criteriaBuilder );
		this.operator = operator;
	}

	/**
	 * Constructs a conjunction or disjunction over the given predicates.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 * @param groupedPredicates The predicates to be grouped.
	 */
	public JpaCompoundPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			BooleanOperator operator,
			List<JpaPredicateImplementor> groupedPredicates) {
		this( criteriaBuilder, operator );
		applyPredicates( groupedPredicates );
	}

	private void applyPredicates(List<JpaPredicateImplementor> groupedPredicates) {
		this.groupedPredicates.clear();
		groupedPredicates.forEach( this::applyPredicate );
	}

	public void applyPredicate(JpaPredicateImplementor predicate) {
		groupedPredicates.add( predicate );
	}

	@Override
	public BooleanOperator getOperator() {
		return operator;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return groupedPredicates.stream().collect( Collectors.toList() );
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		for ( Expression expression : getExpressions() ) {
			Helper.possibleParameter( expression, registry );
		}
	}

	@Override
	public boolean isJunction() {
		return true;
	}

	public static BooleanOperator reverseOperator(BooleanOperator operator) {
		return operator == BooleanOperator.AND
				? BooleanOperator.OR
				: BooleanOperator.AND;
	}

	@Override
	public SqmPredicate visitPredicate(CriteriaVisitor visitor) {
		if ( getOperator() == BooleanOperator.AND ) {
			return visitor.visitAndPredicate(
					groupedPredicates.stream().collect( Collectors.toList() )
			);
		}
		else {
			return visitor.visitOrPredicate(
					groupedPredicates.stream().collect( Collectors.toList() )
			);
		}
	}
}
