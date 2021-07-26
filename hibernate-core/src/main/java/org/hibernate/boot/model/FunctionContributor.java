/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.boot.model;

import org.hibernate.service.ServiceRegistry;

/**
 * Contract for contributing functions
 *
 * @author Karel Maesen
 */
public interface FunctionContributor {

	/**
	 *  Contribute functions
	 *
	 * @param functionContributions The callback for contributing functions
	 * @param serviceRegistry The service registry
	 */
	void contributeFunctions(FunctionContributions functionContributions, ServiceRegistry serviceRegistry);
}
