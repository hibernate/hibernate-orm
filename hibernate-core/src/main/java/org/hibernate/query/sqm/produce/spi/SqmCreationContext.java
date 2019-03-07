/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.service.ServiceRegistry;

/**
 * The "context" object for creation of SQM objects
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationContext {
	MetamodelImplementor getDomainModel();

	ServiceRegistry getServiceRegistry();

	Function<String, SqmFunctionTemplate> getFunctionResolver();
}
