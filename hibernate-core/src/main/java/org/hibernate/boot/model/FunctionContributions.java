/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines the target contributing functions, whether via dialects or {@link FunctionContributor}
 *
 * @author Christian Beikov
 */
public interface FunctionContributions {
	TypeConfiguration getTypeConfiguration();

	SqmFunctionRegistry getFunctionRegistry();

	ServiceRegistry getServiceRegistry();
}
