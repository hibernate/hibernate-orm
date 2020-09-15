/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.map.MapProxyFactory;

/**
 * @author Andrea Boriero
 */
public class DynamicMapProxyBuilder implements ProxyBuilder {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DynamicMapProxyBuilder.class );

	@Override
	public ProxyFactory buildProxyFactory(
			MappedSuperclass mappedSuperclass,
			Getter idGetter,
			Setter idSetter,
			SessionFactoryImplementor sessionFactory) {
		String name = mappedSuperclass.getMappedClass().getName();
		return buildProxyFactory( name );
	}

	@Override
	public ProxyFactory buildProxyFactory(
			PersistentClass persistentClass,
			String entityName,
			Getter idGetter,
			Setter idSetter,
			SessionFactoryImplementor sessionFactory) {
		return buildProxyFactory( entityName );
	}

	private ProxyFactory buildProxyFactory(String name) {
		ProxyFactory pf = new MapProxyFactory();
		try {
			//TODO: design new lifecycle for ProxyFactory
			pf.postInstantiate(
					name,
					null,
					null,
					null,
					null,
					null
			);
		}
		catch (HibernateException he) {
			LOG.unableToCreateProxyFactory( name, he );
			pf = null;
		}
		return pf;
	}
}
