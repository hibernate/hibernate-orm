/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.query.spi.NativeQueryInterpreterInitiator;
import org.hibernate.engine.spi.CacheInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.stat.internal.StatisticsInitiator;

/**
 * Central definition of the standard set of initiators defined by Hibernate for the
 * {@link org.hibernate.service.spi.SessionFactoryServiceRegistry}
 *
 * @author Steve Ebersole
 */
public final class StandardSessionFactoryServiceInitiators {

	public static List<SessionFactoryServiceInitiator<?>> buildStandardServiceInitiatorList() {
		final ArrayList<SessionFactoryServiceInitiator<?>> serviceInitiators = new ArrayList<>();

		serviceInitiators.add( StatisticsInitiator.INSTANCE );
		serviceInitiators.add( CacheInitiator.INSTANCE );
		serviceInitiators.add( NativeQueryInterpreterInitiator.INSTANCE );

		return serviceInitiators;
	}

	private StandardSessionFactoryServiceInitiators() {
	}
}
