/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.loader.criteria.CriteriaJoinWalker;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public abstract class SubqueryExpression implements Criterion {
	
	private CriteriaImpl criteriaImpl;
	private String quantifier;
	private String op;
	private QueryParameters params;
	private Type[] types;
	private CriteriaQueryTranslator innerQuery;

	protected Type[] getTypes() {
		return types;
	}
	
	protected SubqueryExpression(String op, String quantifier, DetachedCriteria dc) {
		this.criteriaImpl = dc.getCriteriaImpl();
		this.quantifier = quantifier;
		this.op = op;
	}
	
	protected abstract String toLeftSqlString(Criteria criteria, CriteriaQuery outerQuery);

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final OuterJoinLoadable persister =
				( OuterJoinLoadable ) factory.getEntityPersister( criteriaImpl.getEntityOrClassName() );

		createAndSetInnerQuery( criteriaQuery, factory );
		criteriaImpl.setSession( deriveRootSession( criteria ) );

		CriteriaJoinWalker walker = new CriteriaJoinWalker(
				persister,
				innerQuery,
				factory,
				criteriaImpl,
				criteriaImpl.getEntityOrClassName(),
				criteriaImpl.getSession().getLoadQueryInfluencers(),
				innerQuery.getRootSQLALias()
		);

		String sql = walker.getSQLString();

		final StringBuffer buf = new StringBuffer( toLeftSqlString(criteria, criteriaQuery) );
		if ( op != null ) {
			buf.append( ' ' ).append( op ).append( ' ' );
		}
		if ( quantifier != null ) {
			buf.append( quantifier ).append( ' ' );
		}
		return buf.append( '(' ).append( sql ).append( ')' )
				.toString();
	}

	private SessionImplementor deriveRootSession(Criteria criteria) {
		if ( criteria instanceof CriteriaImpl ) {
			return ( ( CriteriaImpl ) criteria ).getSession();
		}
		else if ( criteria instanceof CriteriaImpl.Subcriteria ) {
			return deriveRootSession( ( ( CriteriaImpl.Subcriteria ) criteria ).getParent() );
		}
		else {
			// could happen for custom Criteria impls.  Not likely, but...
			// 		for long term solution, see HHH-3514
			return null;
		}
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		//the following two lines were added to ensure that this.params is not null, which
		//can happen with two-deep nested subqueries
		SessionFactoryImplementor factory = criteriaQuery.getFactory();
		createAndSetInnerQuery(criteriaQuery, factory);
		
		Type[] ppTypes = params.getPositionalParameterTypes();
		Object[] ppValues = params.getPositionalParameterValues();
		TypedValue[] tv = new TypedValue[ppTypes.length];
		for ( int i=0; i<ppTypes.length; i++ ) {
			tv[i] = new TypedValue( ppTypes[i], ppValues[i], EntityMode.POJO );
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
					criteriaImpl.getEntityOrClassName(), //implicit polymorphism not supported (would need a union)
					alias,
					criteriaQuery
				);

			params = innerQuery.getQueryParameters();
			types = innerQuery.getProjectedTypes();
		}
	}
}
