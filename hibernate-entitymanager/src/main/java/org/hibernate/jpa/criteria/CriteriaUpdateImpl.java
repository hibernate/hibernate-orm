/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.criteria;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.jpa.criteria.compile.RenderingContext;
import org.hibernate.jpa.criteria.path.SingularAttributePath;

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
		if ( ! PathImplementor.class.isInstance( attributePath ) ) {
			throw new IllegalArgumentException( "Unexpected path implementation type : " + attributePath.getClass().getName() );
		}
		if ( ! SingularAttributePath.class.isInstance( attributePath ) ) {
			throw new IllegalArgumentException(
					"Attribute path for assignment must represent a singular attribute ["
							+ ( (PathImplementor) attributePath ).getPathIdentifier() + "]"
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

	@Override
	protected String renderQuery(RenderingContext renderingContext) {
		final StringBuilder jpaql = new StringBuilder( "update " );
		renderRoot( jpaql, renderingContext );
		renderAssignments( jpaql, renderingContext );
		renderRestrictions( jpaql, renderingContext );

		return jpaql.toString();
	}

	private void renderAssignments(StringBuilder jpaql, RenderingContext renderingContext) {
		jpaql.append( " set " );
		boolean first = true;
		for ( Assignment assignment : assignments ) {
			if ( ! first ) {
				jpaql.append( ", " );
			}
			jpaql.append( assignment.attributePath.render( renderingContext ) )
					.append( " = " )
					.append( assignment.value.render( renderingContext ) );
			first = false;
		}
	}

	private class Assignment<A> {
		private final SingularAttributePath<A> attributePath;
		private final ExpressionImplementor<? extends A> value;

		private Assignment(SingularAttributePath<A> attributePath, Expression<? extends A> value) {
			this.attributePath = attributePath;
			this.value = (ExpressionImplementor) value;
		}
	}
}
