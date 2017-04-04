/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;

/**
 * A service provided as-is.
 *
 * @author Steve Ebersole
 */
public class ProvidedService<R> {
	private final Class<R> serviceRole;
	private final R service;

	public ProvidedService(Class<R> serviceRole, R service) {
		this.serviceRole = serviceRole;
		this.service = service;
	}

	public Class<R> getServiceRole() {
		return serviceRole;
	}

	public R getService() {
		return service;
	}
}
