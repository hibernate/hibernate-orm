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
