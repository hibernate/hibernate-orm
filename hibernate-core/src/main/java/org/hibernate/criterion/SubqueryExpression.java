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
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.criteria.CriteriaJoinWalker;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.Type;

/**
 * A criterion that involves a subquery
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public abstract class SubqueryExpression implements Criterion {
	private CriteriaImpl criteriaImpl;
	private String quantifier;
	private String op;
	private QueryParameters params;
	private Type[] types;
	private CriteriaQueryTranslator innerQuery;

	protected SubqueryExpression(String op, String quantifier, DetachedCriteria dc) {
		this.criteriaImpl = dc.getCriteriaImpl();
		this.quantifier = quantifier;
		this.op = op;
	}

	protected Type[] getTypes() {
		return types;
	}

	protected abstract String toLeftSqlString(Criteria criteria, CriteriaQuery outerQuery);

	@Override
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final StringBuilder buf = new StringBuilder( toLeftSqlString( criteria, criteriaQuery ) );
		if ( op != null ) {
			buf.append( ' ' ).append( op ).append( ' ' );
		}
		if ( quantifier != null ) {
			buf.append( quantifier ).append( ' ' );
		}

		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final OuterJoinLoadable persister =
				(OuterJoinLoadable) factory.getEntityPersister( criteriaImpl.getEntityOrClassName() );

		createAndSetInnerQuery( criteriaQuery, factory );
		criteriaImpl.setSession( deriveRootSession( criteria ) );

		final CriteriaJoinWalker walker = new CriteriaJoinWalker(
				persister,
				innerQuery,
				factory,
				criteriaImpl,
				criteriaImpl.getEntityOrClassName(),
				criteriaImpl.getSession().getLoadQueryInfluencers(),
				innerQuery.getRootSQLALias()
		);

		return buf.append( '(' ).append( walker.getSQLString() ).append( ')' ).toString();
	}

	private SessionImplementor deriveRootSession(Criteria criteria) {
		if ( criteria instanceof CriteriaImpl ) {
			return ( (CriteriaImpl) criteria ).getSession();
		}
		else if ( criteria instanceof CriteriaImpl.Subcriteria ) {
			return deriveRootSession( ( (CriteriaImpl.Subcriteria) criteria ).getParent() );
		}
		else {
			// could happen for custom Criteria impls.  Not likely, but...
			// 		for long term solution, see HHH-3514
			return null;
		}
	}

	@Override
	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		//the following two lines were added to ensure that this.params is not null, which
		//can happen with two-deep nested subqueries
		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		createAndSetInnerQuery( criteriaQuery, factory );

		final Type[] ppTypes = params.getPositionalParameterTypes();
		final Object[] ppValues = params.getPositionalParameterValues();
		final TypedValue[] tv = new TypedValue[ppTypes.length];
		for ( int i=0; i<ppTypes.length; i++ ) {
			tv[i] = new TypedValue( ppTypes[i], ppValues[i] );
		}
		return tv;
	}

	/**
	 * Creates the inner query used to extract some useful information about types, since it is needed in both methods.
	 *
	 * @param criteriaQuery The criteria query
	 * @param factory The session factory.
	 */
	private void createAndSetInnerQuery(CriteriaQuery criteriaQuery, SessionFactoryImplementor factory) {
		if ( innerQuery == null ) {
			//with two-deep subqueries, the same alias would get generated for
			//both using the old method (criteriaQuery.generateSQLAlias()), so
			//that is now used as a fallback if the main criteria alias isn't set
			String alias;
			if ( this.criteriaImpl.getAlias() == null ) {
				alias = criteriaQuery.generateSQLAlias();
			}
			else {
				alias = this.criteriaImpl.getAlias() + "_";
			}

			innerQuery = new CriteriaQueryTranslator(
					factory,
					criteriaImpl,
					criteriaImpl.getEntityOrClassName(),
					alias,
					criteriaQuery
				);

			params = innerQuery.getQueryParameters();
			types = innerQuery.getProjectedTypes();
		}
	}
}
