/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.service.ServiceRegistry;

/**
 * The "context" in which creation of SQL AST occurs.  Exposes access to
 * services generally needed in creating SQL AST nodes
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationContext {
	SessionFactoryImplementor getSessionFactory();

	DomainMetamodel getDomainModel();

	ServiceRegistry getServiceRegistry();

	Integer getMaximumFetchDepth();
}
