/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ejb.test.instrument.cases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.bytecode.spi.InstrumentedClassLoader;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.service.BootstrapServiceRegistryBuilder;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractExecutable implements Executable {
	private static final Dialect dialect = Dialect.getDialect();
	private EntityManagerFactory entityManagerFactory;
	private EntityManager em;

    @Override
	public final void prepare() {
		// make sure we pick up the TCCL, and make sure its the isolated CL...
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader == null ) {
			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
		}
		if ( !InstrumentedClassLoader.class.isInstance( classLoader ) ) {
			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
		}

		Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
		ejb3Configuration.configure( buildSettings() );
		final BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
		bootstrapServiceRegistryBuilder.with( classLoader );
		entityManagerFactory = ejb3Configuration.buildEntityManagerFactory( bootstrapServiceRegistryBuilder );
	}

    @Override
	public final void complete() {
		try {
			cleanup();
		}
		finally {
			if ( em != null && em.isOpen() ) {
				em.close();
			}
			em = null;
			entityManagerFactory.close();
			entityManagerFactory = null;
		}
	}

	protected EntityManager getOrCreateEntityManager() {
		if ( em == null || !em.isOpen() ) {
			em = entityManagerFactory.createEntityManager();
		}
		return em;
	}

	protected void cleanup() {
	}

	private Map buildSettings() {
		Map<Object, Object> settings = Environment.getProperties();
		ArrayList<Class> classes = new ArrayList<Class>();
		classes.addAll( Arrays.asList( getAnnotatedClasses() ) );
		settings.put( AvailableSettings.LOADED_CLASSES, classes );
		settings.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		settings.put( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
		settings.put( org.hibernate.cfg.AvailableSettings.DIALECT, dialect.getClass().getName() );
		return settings;
	}
}
