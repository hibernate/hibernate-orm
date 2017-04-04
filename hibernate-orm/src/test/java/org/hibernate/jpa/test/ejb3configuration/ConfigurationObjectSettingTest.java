/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ejb3configuration;

import java.util.Collections;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;

import org.hibernate.HibernateException;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitInfoAdapter;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test passing along various config settings that take objects other than strings as values.
 *
 * @author Steve Ebersole
 */
public class ConfigurationObjectSettingTest extends BaseUnitTestCase {
	@Test
	public void testContainerBootstrapSharedCacheMode() {
		// first, via the integration vars
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		{
			// as object
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE, builder.getConfigurationValues().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}
		{
			// as string
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE.name() )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE.name(), builder.getConfigurationValues().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}

		// next, via the PUI
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public SharedCacheMode getSharedCacheMode() {
				return SharedCacheMode.ENABLE_SELECTIVE;
			}
		};
		{
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					null
			);
			assertEquals( SharedCacheMode.ENABLE_SELECTIVE, builder.getConfigurationValues().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}

		// via both, integration vars should take precedence
		{
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE, builder.getConfigurationValues().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}
	}

	@Test
	public void testContainerBootstrapValidationMode() {
		// first, via the integration vars
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		{
			// as object
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.CALLBACK )
			);
			assertEquals( ValidationMode.CALLBACK, builder.getConfigurationValues().get( AvailableSettings.VALIDATION_MODE ) );
		}
		{
			// as string
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					empty,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.CALLBACK.name() )
			);
			assertEquals( ValidationMode.CALLBACK.name(), builder.getConfigurationValues().get( AvailableSettings.VALIDATION_MODE ) );
		}

		// next, via the PUI
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public ValidationMode getValidationMode() {
				return ValidationMode.CALLBACK;
			}
		};
		{
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					null
			);
			assertEquals( ValidationMode.CALLBACK, builder.getConfigurationValues().get( AvailableSettings.VALIDATION_MODE ) );
		}

		// via both, integration vars should take precedence
		{
			EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.NONE )
			);
			assertEquals( ValidationMode.NONE, builder.getConfigurationValues().get( AvailableSettings.VALIDATION_MODE ) );
		}
	}

	@Test
	public void testContainerBootstrapValidationFactory() {
		final Object token = new Object();
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
		try {
			Bootstrap.getEntityManagerFactoryBuilder(
					adapter,
					Collections.singletonMap( AvailableSettings.VALIDATION_FACTORY, token )
			);
			fail( "Was expecting error as token did not implement ValidatorFactory" );
		}
		catch ( HibernateException e ) {
			// probably the condition we want but unfortunately the exception is not specific
			// and the pertinent info is in a cause
		}
	}
}
