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
import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.ResultTransformer;

/**
 * Some applications need to create criteria queries in "detached
 * mode", where the Hibernate session is not available. This class
 * may be instantiated anywhere, and then a <literal>Criteria</literal>
 * may be obtained by passing a session to 
 * <literal>getExecutableCriteria()</literal>. All methods have the
 * same semantics and behavior as the corresponding methods of the
 * <literal>Criteria</literal> interface.
 * 
 * @see org.hibernate.Criteria
 * @author Gavin King
 */
public class DetachedCriteria implements CriteriaSpecification, Serializable {
	
	private final CriteriaImpl impl;
	private final Criteria criteria;
	
	protected DetachedCriteria(String entityName) {
		impl = new CriteriaImpl(entityName, null);
		criteria = impl;
	}
	
	protected DetachedCriteria(String entityName, String alias) {
		impl = new CriteriaImpl(entityName, alias, null);
		criteria = impl;
	}
	
	protected DetachedCriteria(CriteriaImpl impl, Criteria criteria) {
		this.impl = impl;
		this.criteria = criteria;
	}
	
	/**
	 * Get an executable instance of <literal>Criteria</literal>,
	 * to actually run the query.
	 */
	public Criteria getExecutableCriteria(Session session) {
		impl.setSession( ( SessionImplementor ) session );
		return impl;
	}
	
	public static DetachedCriteria forEntityName(String entityName) {
		return new DetachedCriteria(entityName);
	}
	
	public static DetachedCriteria forEntityName(String entityName, String alias) {
		return new DetachedCriteria(entityName, alias);
	}
	
	public static DetachedCriteria forClass(Class clazz) {
		return new DetachedCriteria( clazz.getName() );
	}
	
	public static DetachedCriteria forClass(Class clazz, String alias) {
		return new DetachedCriteria( clazz.getName() , alias );
	}
	
	public DetachedCriteria add(Criterion criterion) {
		criteria.add(criterion);
		return this;
	}

	public DetachedCriteria addOrder(Order order) {
		criteria.addOrder(order);
		return this;
	}

	public DetachedCriteria createAlias(String associationPath, String alias)
	throws HibernateException {
		criteria.createAlias(associationPath, alias);
		return this;
	}

	public DetachedCriteria createCriteria(String associationPath, String alias)
	throws HibernateException {
		return new DetachedCriteria( impl, criteria.createCriteria(associationPath, alias) );
	}

	public DetachedCriteria createCriteria(String associationPath)
	throws HibernateException {
		return new DetachedCriteria( impl, criteria.createCriteria(associationPath) );
	}

	public String getAlias() {
		return criteria.getAlias();
	}

	public DetachedCriteria setFetchMode(String associationPath, FetchMode mode)
	throws HibernateException {
		criteria.setFetchMode(associationPath, mode);
		return this;
	}

	public DetachedCriteria setProjection(Projection projection) {
		criteria.setProjection(projection);
		return this;
	}

	public DetachedCriteria setResultTransformer(ResultTransformer resultTransformer) {
		criteria.setResultTransformer(resultTransformer);
		return this;
	}
	
	public String toString() {
		return "DetachableCriteria(" + criteria.toString() + ')';
	}
	
	CriteriaImpl getCriteriaImpl() {
		return impl;
	}

    public DetachedCriteria createAlias(String associationPath, String alias, JoinType joinType) throws HibernateException {
        criteria.createAlias(associationPath, alias, joinType);
        return this;
    }
	
	public DetachedCriteria createAlias(String associationPath, String alias, JoinType joinType, Criterion withClause) throws HibernateException {
		criteria.createAlias(associationPath, alias, joinType, withClause);
		return this;
	}
	
	public DetachedCriteria createCriteria(String associationPath, JoinType joinType) throws HibernateException {
        return new DetachedCriteria(impl, criteria.createCriteria(associationPath, joinType));
    }

    public DetachedCriteria createCriteria(String associationPath, String alias, JoinType joinType) throws HibernateException {
        return new DetachedCriteria(impl, criteria.createCriteria(associationPath, alias, joinType));
    }
	
	public DetachedCriteria createCriteria(String associationPath, String alias, JoinType joinType, Criterion withClause) throws HibernateException {
		return new DetachedCriteria(impl, criteria.createCriteria(associationPath, alias, joinType, withClause));
	}

	/**
	 * @deprecated use {@link #createAlias(String, String, JoinType)}
	 */
	@Deprecated
	public DetachedCriteria createAlias(String associationPath, String alias, int joinType) throws HibernateException {
       return createAlias( associationPath, alias, JoinType.parse( joinType ) );
    }
	/**
	 * @deprecated use {@link #createAlias(String, String, JoinType, Criterion)}
	 */
	@Deprecated
	public DetachedCriteria createAlias(String associationPath, String alias, int joinType, Criterion withClause) throws HibernateException {
		return createAlias( associationPath, alias, JoinType.parse( joinType ), withClause );
	}
	/**
	 * @deprecated use {@link #createCriteria(String, JoinType)}
	 */
	@Deprecated
	public DetachedCriteria createCriteria(String associationPath, int joinType) throws HibernateException {
        return createCriteria( associationPath, JoinType.parse( joinType ) );
    }
	/**
	 * @deprecated use {@link #createCriteria(String, String, JoinType)}
	 */
	@Deprecated
    public DetachedCriteria createCriteria(String associationPath, String alias, int joinType) throws HibernateException {
        return createCriteria( associationPath, alias, JoinType.parse( joinType ) );
    }
	/**
	 * @deprecated use {@link #createCriteria(String, String, JoinType, Criterion)}
	 */
	@Deprecated
	public DetachedCriteria createCriteria(String associationPath, String alias, int joinType, Criterion withClause) throws HibernateException {
		return createCriteria( associationPath, alias, JoinType.parse( joinType ), withClause );
	}
	
	public DetachedCriteria setComment(String comment) {
        criteria.setComment(comment);
        return this;
    }

    public DetachedCriteria setLockMode(LockMode lockMode) {
        criteria.setLockMode(lockMode);
        return this;
    }

    public DetachedCriteria setLockMode(String alias, LockMode lockMode) {
        criteria.setLockMode(alias, lockMode);
        return this;
    }
}
