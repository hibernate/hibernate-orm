/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.service.ServiceRegistry;

/**
 * Contract for contributing types.
 *
 * @author Steve Ebersole
 * 
 * NOTE: Cherry-pick of HHH-7998 from metamodel.  For merging simplicity, just
 * keep it in the o.h.metamodel.spi package.
 */
public interface TypeContributor {
	/**
	 * Contribute types
	 *
	 * @param typeContributions The callback for adding contributed types
	 * @param serviceRegistry The service registry
	 */
	void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry);
}
