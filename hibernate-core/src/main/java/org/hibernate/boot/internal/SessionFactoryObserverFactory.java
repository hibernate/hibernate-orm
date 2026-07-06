/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.bytecode.internal.SessionFactoryObserverForBytecodeEnhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;

/**
 * Creates ORM's built-in SessionFactory lifecycle observers.
 *
 * @author Steve Ebersole
 */
public final class SessionFactoryObserverFactory {
	private SessionFactoryObserverFactory() {
	}

	public static SessionFactoryObserver[] createObservers(MetadataImplementor metadata) {
		final var bytecodeProvider = metadata.getMappingResolutionOptions()
				.getServiceRegistry()
				.getService( BytecodeProvider.class );
		return new SessionFactoryObserver[] {
				new SessionFactoryObserverForBytecodeEnhancer( bytecodeProvider ),
				new SessionFactoryObserverForNamedQueryValidation( metadata ),
				new SessionFactoryObserverForSchemaExport( metadata ),
				new SessionFactoryObserverForRegistration()
		};
	}
}
