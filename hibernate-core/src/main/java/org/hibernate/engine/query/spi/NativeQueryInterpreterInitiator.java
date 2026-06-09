/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

/**
 * @author Steve Ebersole
 */
public class NativeQueryInterpreterInitiator implements SessionFactoryServiceInitiator<NativeQueryInterpreter> {
	/**
	 * Singleton access
	 */
	public static final NativeQueryInterpreterInitiator INSTANCE = new NativeQueryInterpreterInitiator();

	@Override
	@Nonnull
	public NativeQueryInterpreter initiateService(@Nonnull SessionFactoryServiceInitiatorContext context) {
		return new NativeQueryInterpreterStandardImpl( context.getSessionFactoryOptions().getNativeJdbcParametersIgnored() );
	}

	@Override
	@Nonnull
	public Class<NativeQueryInterpreter> getServiceInitiated() {
		return NativeQueryInterpreter.class;
	}
}
