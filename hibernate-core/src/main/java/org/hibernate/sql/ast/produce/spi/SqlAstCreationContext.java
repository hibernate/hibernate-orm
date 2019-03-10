/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.service.ServiceRegistry;

/**
 * The "context" in which creation of SQL AST occurs.
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationContext {
	MetamodelImplementor getDomainModel();

	ServiceRegistry getServiceRegistry();

	Integer getMaximumFetchDepth();
}
