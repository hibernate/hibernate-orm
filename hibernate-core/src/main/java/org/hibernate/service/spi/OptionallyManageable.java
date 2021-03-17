/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.service.spi;

import java.util.List;

/**
 * Extension to Manageable for things that are optionally Manageable depending
 * on some internal state.  E.g. services that wrap other services wanting to
 * delegate manageability if the wrapped service is Manageable.
 *
 * @author Steve Ebersole
 */
public interface OptionallyManageable extends Manageable {
	/**
	 * Any wrapped services that are Manageable.  Never return `null`; an empty
	 * List should be returned instead.
	 */
	List<Manageable> getRealManageables();

	@Override
	default Object getManagementBean() {
		// Generally the wrapper is not Manageable itself
		return null;
	}
}
