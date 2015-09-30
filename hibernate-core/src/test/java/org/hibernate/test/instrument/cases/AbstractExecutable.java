/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.cases;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.spi.InstrumentedClassLoader;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractExecutable implements Executable {
	private ServiceRegistry serviceRegistry;
	private SessionFactory factory;

    @Override
	public final void prepare() {
		BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder();
		// make sure we pick up the TCCL, and make sure its the isolated CL...
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader == null ) {
			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
		}
		if ( !InstrumentedClassLoader.class.isInstance( classLoader ) ) {
			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
		}
		bsrb.applyClassLoader( classLoader );

		serviceRegistry = new StandardServiceRegistryBuilder( bsrb.build() )
				.applySetting( Environment.HBM2DDL_AUTO, "create-drop" )
				.build();

		MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		for ( String resource : getResources() ) {
			metadataSources.addResource( resource );
		}

		factory = metadataSources.buildMetadata().buildSessionFactory();
	}

    @Override
	public final void complete() {
		try {
			cleanup();
		}
		finally {
			factory.close();
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	protected SessionFactory getFactory() {
		return factory;
	}

	protected void cleanup() {
	}

	protected String[] getResources() {
		return new String[] { "org/hibernate/test/instrument/domain/Documents.hbm.xml" };
	}
}
