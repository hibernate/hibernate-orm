/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.query.internal.sql.NativeQueryInterpreterStandardImpl;
import org.hibernate.query.spi.NativeQueryInterpreter;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;

/**
 * @author Steve Ebersole
 */
public class NativeQueryInterpreterInitiator implements SessionFactoryServiceInitiator<NativeQueryInterpreter > {
	/**
	 * Singleton access
	 */
	public static final NativeQueryInterpreterInitiator INSTANCE = new NativeQueryInterpreterInitiator();

	@Override
	public NativeQueryInterpreter initiateService(SessionFactoryServiceInitiatorContext context) {
		return NativeQueryInterpreterStandardImpl.INSTANCE;
	}

	@Override
	public Class<org.hibernate.query.spi.NativeQueryInterpreter > getServiceInitiated() {
		return NativeQueryInterpreter .class;
	}
}
