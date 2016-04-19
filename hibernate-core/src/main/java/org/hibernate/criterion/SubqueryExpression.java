/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
				(OuterJoinLoadable) factory.getMetamodel().entityPersister( criteriaImpl.getEntityOrClassName() );

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

	private SharedSessionContractImplementor deriveRootSession(Criteria criteria) {
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
