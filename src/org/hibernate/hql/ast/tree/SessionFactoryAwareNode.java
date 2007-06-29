package org.hibernate.hql.ast.tree;

import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Interface for nodes which require access to the SessionFactory
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryAwareNode {
	public void setSessionFactory(SessionFactoryImplementor sessionFactory);
}
