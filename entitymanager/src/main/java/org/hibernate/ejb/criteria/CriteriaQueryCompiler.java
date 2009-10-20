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
package org.hibernate.ejb.criteria;

import java.util.Set;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.ejb.HibernateEntityManagerImplementor;

/**
 * Compiles a JPA criteria query into an executable {@link TypedQuery}.  Its single contract is the {@link #compile}
 * method.
 * <p/>
 * NOTE : This is a temporary implementation which simply translates the criteria query into a JPAQL query string.  A
 * better, long-term solution is being implemented as part of refactoring the JPAQL/HQL translator.
 *
 * @author Steve Ebersole
 */
public class CriteriaQueryCompiler {
	private final HibernateEntityManagerImplementor entityManager;

	public CriteriaQueryCompiler(HibernateEntityManagerImplementor entityManager) {
		this.entityManager = entityManager;
	}

	public <T> TypedQuery<T> compile(CriteriaQuery<T> criteriaQuery) {
		CriteriaQueryImpl<T> criteriaQueryImpl = ( CriteriaQueryImpl<T> ) criteriaQuery;

		criteriaQueryImpl.validate();
		Set<ParameterExpression<?>> explicitParameters = criteriaQueryImpl.getParameters();
		// todo : implicit parameter handling (handling literal as param, etc).
		String jpaqlEquivalent = criteriaQueryImpl.render();



		return null;
	}
}
