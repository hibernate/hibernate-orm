/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.service.Service;

/**
 * Contract for an initiator of services that target the specialized service registry
 * {@link SessionFactoryServiceRegistry}.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceInitiator<R extends Service> extends ServiceInitiator<R>{
	/**
	 * Initiates the managed service.
	 * <p>
	 * Note for implementors: signature is guaranteed to change once redesign of SessionFactory building is complete
	 *
	 * @param context Access to initialization contextual info
	 *
	 * @return The initiated service.
	 */
	R initiateService(SessionFactoryServiceInitiatorContext context);
}
