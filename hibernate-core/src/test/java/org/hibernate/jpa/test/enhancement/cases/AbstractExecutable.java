/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement.cases;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestTask;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractExecutable implements EnhancerTestTask {
	private static final Dialect dialect = Dialect.getDialect();
	private HibernateEntityManagerFactory entityManagerFactory;
	private EntityManager em;

    @Override
	public final void prepare() {
		// make sure we pick up the TCCL, and make sure its the isolated CL...
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader == null ) {
			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
		}
//		if ( !InstrumentedClassLoader.class.isInstance( classLoader ) ) {
//			throw new RuntimeException( "Isolated ClassLoader not yet set as TCCL" );
//		}

		entityManagerFactory =  Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor( getClass().getSimpleName() ),
				buildSettings(),
				classLoader
		).build().unwrap( HibernateEntityManagerFactory.class );

		prepared();
	}

	protected void prepared() {

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

	protected HibernateEntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
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

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor(final String puName) {
		return new PersistenceUnitDescriptor() {
			private final String name = puName;

			@Override public URL getPersistenceUnitRootUrl() {
				return null;
			}

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getProviderClassName() {
				return HibernatePersistenceProvider.class.getName();
			}

			@Override
			public boolean isUseQuotedIdentifiers() {
				return false;
			}

			@Override
			public boolean isExcludeUnlistedClasses() {
				return false;
			}

			@Override
			public PersistenceUnitTransactionType getTransactionType() {
				return null;
			}

			@Override
			public ValidationMode getValidationMode() {
				return null;
			}

			@Override
			public SharedCacheMode getSharedCacheMode() {
				return null;
			}

			@Override
			public List<String> getManagedClassNames() {
				return null;
			}

			@Override
			public List<String> getMappingFileNames() {
				return null;
			}

			@Override
			public List<URL> getJarFileUrls() {
				return null;
			}

			@Override
			public Object getNonJtaDataSource() {
				return null;
			}

			@Override
			public Object getJtaDataSource() {
				return null;
			}

			@Override
			public Properties getProperties() {
				return null;
			}

			@Override
			public ClassLoader getClassLoader() {
				return null;
			}

			@Override
			public ClassLoader getTempClassLoader() {
				return null;
			}

			@Override
			public void pushClassTransformer(EnhancementContext enhancementContext) {
			}
		};
	}
}
