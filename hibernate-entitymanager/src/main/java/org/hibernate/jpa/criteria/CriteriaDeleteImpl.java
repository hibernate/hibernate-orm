/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;

import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * Hibernate implementation of the JPA 2.1 {@link CriteriaDelete} contract.
 *
 * @author Steve Ebersole
 */
public class CriteriaDeleteImpl<T> extends AbstractManipulationCriteriaQuery<T> implements CriteriaDelete<T> {
	protected CriteriaDeleteImpl(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder );
	}

	@Override
	public CriteriaDelete<T> where(Expression<Boolean> restriction) {
		setRestriction( restriction );
		return this;
	}

	@Override
	public CriteriaDelete<T> where(Predicate... restrictions) {
		setRestriction( restrictions );
		return this;
	}

	@Override
	protected String renderQuery(RenderingContext renderingContext) {
		final StringBuilder jpaql = new StringBuilder( "delete " );
		renderRoot( jpaql, renderingContext );
		renderRestrictions( jpaql, renderingContext );

		return jpaql.toString();
	}
}
