//$Id: DetachedCriteria.java 8757 2005-12-06 03:35:50Z steveebersole $
package org.hibernate.criterion;

import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.impl.SessionImpl;
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
		return new DetachedCriteria( impl, criteria.createCriteria(associationPath) );
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
}
