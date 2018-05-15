/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.mode;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ProxyFactoryInstantiator;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.map.MapProxyFactory;

/**
 * @author Chris Cranford
 */
public class StandardMapProxyFactoryInstantiator<J> implements ProxyFactoryInstantiator<J> {
	public static final StandardMapProxyFactoryInstantiator INSTANCE = new StandardMapProxyFactoryInstantiator();

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StandardMapProxyFactoryInstantiator.class );

	// todo (6.0) - We may want to merge this into StandardMapRepresentationStrategy?

	@Override
	public ProxyFactory instantiate(
			EntityTypeDescriptor<J> runtimeDescriptor,
			RuntimeModelCreationContext creationContext) {

		ProxyFactory pf = new MapProxyFactory();
		try {
			//TODO: design new lifecycle for ProxyFactory
			pf.postInstantiate(
					runtimeDescriptor.getEntityName(),
					null,
					null,
					null,
					null,
					null
			);
		}
		catch ( HibernateException he) {
			LOG.unableToCreateProxyFactory( runtimeDescriptor.getEntityName(), he );
			pf = null;
		}
		return pf;
	}

	private StandardMapProxyFactoryInstantiator() {

	}
}
