/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
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
	public NativeQueryInterpreter initiateService(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry) {
		return new NativeQueryInterpreterStandardImpl( sessionFactory );
	}

	@Override
	public NativeQueryInterpreter initiateService(SessionFactoryServiceInitiatorContext context) {
		return new NativeQueryInterpreterStandardImpl( context.getSessionFactory() );
	}

	@Override
	public Class<NativeQueryInterpreter> getServiceInitiated() {
		return NativeQueryInterpreter.class;
	}
}
