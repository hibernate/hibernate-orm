/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.engine.spi.SessionFactoryImplementor;

public final class SessionFactoryUtil {
	private SessionFactoryUtil() {
	}

	public static SessionFactoryImplementor buildSessionFactory(Metadata metadata) {
		return SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() );
	}
}
