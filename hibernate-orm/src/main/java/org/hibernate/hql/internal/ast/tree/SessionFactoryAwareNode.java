/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Interface for nodes which require access to the SessionFactory
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryAwareNode {
	public void setSessionFactory(SessionFactoryImplementor sessionFactory);
}
