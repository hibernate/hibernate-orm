/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.query.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.query.internal.NativeQueryInterpreterStandardImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

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
			Configuration configuration,
			ServiceRegistryImplementor registry) {
		return NativeQueryInterpreterStandardImpl.INSTANCE;
	}

	@Override
	public NativeQueryInterpreter initiateService(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata,
			ServiceRegistryImplementor registry) {
		return NativeQueryInterpreterStandardImpl.INSTANCE;
	}

	@Override
	public Class<NativeQueryInterpreter> getServiceInitiated() {
		return NativeQueryInterpreter.class;
	}
}
