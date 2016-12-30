/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.query.criteria.AbstractManipulationCriteriaQuery;
import org.hibernate.query.criteria.JpaExpressionImplementor;
import org.hibernate.query.criteria.JpaPathImplementor;
import org.hibernate.query.criteria.internal.path.SingularAttributePath;

/**
 * Hibernate implementation of the JPA 2.1 {@link CriteriaUpdate} contract.
 *
 * @author Steve Ebersole
 */
public class CriteriaUpdateImpl<T> extends AbstractManipulationCriteriaQuery<T> implements CriteriaUpdate<T> {
	private List<Assignment> assignments = new ArrayList<Assignment>();

	public CriteriaUpdateImpl(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y, X extends Y> CriteriaUpdate<T> set(SingularAttribute<? super T, Y> singularAttribute, X value) {
		final Path<Y> attributePath = getRoot().get( singularAttribute );
		final Expression valueExpression = value == null
				? criteriaBuilder().nullLiteral( attributePath.getJavaType() )
				: criteriaBuilder().literal( value );
		addAssignment( attributePath, valueExpression );
		return this;
	}

	@Override
	public <Y> CriteriaUpdate<T> set(
			SingularAttribute<? super T, Y> singularAttribute,
			Expression<? extends Y> value) {
		addAssignment( getRoot().get( singularAttribute ), value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y, X extends Y> CriteriaUpdate<T> set(Path<Y> attributePath, X value) {
		final Expression valueExpression = value == null
				? criteriaBuilder().nullLiteral( attributePath.getJavaType() )
				: criteriaBuilder().literal( value );
		addAssignment( attributePath, valueExpression );
		return this;
	}

	@Override
	public <Y> CriteriaUpdate<T> set(Path<Y> attributePath, Expression<? extends Y> value) {
		addAssignment( attributePath, value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CriteriaUpdate<T> set(String attributeName, Object value) {
		final Path attributePath = getRoot().get( attributeName );
		final Expression valueExpression = value == null
				? criteriaBuilder().nullLiteral( attributePath.getJavaType() )
				: criteriaBuilder().literal( value );
		addAssignment( attributePath, valueExpression );
		return this;
	}

	protected <Y> void addAssignment(Path<Y> attributePath, Expression<? extends Y> value) {
		if ( ! JpaPathImplementor.class.isInstance( attributePath ) ) {
			throw new IllegalArgumentException( "Unexpected path implementation type : " + attributePath.getClass().getName() );
		}
		if ( ! SingularAttributePath.class.isInstance( attributePath ) ) {
			throw new IllegalArgumentException(
					"Attribute path for assignment must represent a singular attribute ["
							+ ( (JpaPathImplementor) attributePath ).getPathIdentifier() + "]"
			);
		}
		if ( value == null ) {
			throw new IllegalArgumentException( "Assignment value expression cannot be null. Did you mean to pass null as a literal?" );
		}
		assignments.add( new Assignment<Y>( (SingularAttributePath<Y>) attributePath, value ) );
	}

	@Override
	public CriteriaUpdate<T> where(Expression<Boolean> restriction) {
		setRestriction( restriction );
		return this;
	}

	@Override
	public CriteriaUpdate<T> where(Predicate... restrictions) {
		setRestriction( restrictions );
		return this;
	}

	@Override
	public void validate() {
		super.validate();
		if ( assignments.isEmpty() ) {
			throw new IllegalStateException( "No assignments specified as part of UPDATE criteria" );
		}
	}

	private static class Assignment<A> {
		private final SingularAttributePath<A> attributePath;
		private final JpaExpressionImplementor<? extends A> value;

		private Assignment(SingularAttributePath<A> attributePath, Expression<? extends A> value) {
			this.attributePath = attributePath;
			this.value = (JpaExpressionImplementor) value;
		}
	}
}
