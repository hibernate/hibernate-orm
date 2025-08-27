/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query.spi;

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
	public NativeQueryInterpreter initiateService(SessionFactoryServiceInitiatorContext context) {
		return new NativeQueryInterpreterStandardImpl( context.getSessionFactoryOptions().getNativeJdbcParametersIgnored() );
	}

	@Override
	public Class<NativeQueryInterpreter> getServiceInitiated() {
		return NativeQueryInterpreter.class;
	}
}
