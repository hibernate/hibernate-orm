/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public interface HandlerExecutionContext extends ExecutionContext, SqlAstCreationContext {

	// todo (6.0) : is this needed at all?
	//		it seems to exist just to "bind" ExecutionContext and SqlAstCreationContext
	//		together, though the users would already have access to SqlAstCreationContext
	//		via SessionFactory (which implements SqlAstCreationContext)

	default SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}

	@Override
	default MetamodelImplementor getDomainModel() {
		return getSessionFactory().getMetamodel();
	}

	@Override
	default ServiceRegistry getServiceRegistry() {
		return getSessionFactory().getServiceRegistry();
	}

	@Override
	default Integer getMaximumFetchDepth() {
		return getSessionFactory().getMaximumFetchDepth();
	}
}
