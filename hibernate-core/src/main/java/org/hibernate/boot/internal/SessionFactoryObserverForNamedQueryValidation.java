/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.QueryEngine;

/**
 * Responsible for triggering {@linkplain QueryEngine#validateNamedQueries()
 * named query validation} when the {@link SessionFactory} is created.
 *
 * @implNote This was added in order to clean up the constructor of
 *           {@link org.hibernate.internal.SessionFactoryImpl}, which
 *           was doing too many things.
 *
 * @author Gavin King
 */
class SessionFactoryObserverForNamedQueryValidation implements SessionFactoryObserver {
	private final Metadata metadata;

	SessionFactoryObserverForNamedQueryValidation(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		final SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) factory;
		final QueryEngine queryEngine = sessionFactory.getQueryEngine();
		queryEngine.getNamedObjectRepository().prepare( sessionFactory, metadata );
		if ( sessionFactory.getSessionFactoryOptions().isNamedQueryStartupCheckingEnabled() ) {
			queryEngine.validateNamedQueries();
		}
	}
}
