/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.test.ejb3configuration;

import java.util.Collections;
import java.util.Map;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;

import org.hibernate.HibernateException;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.packaging.PersistenceMetadata;

/**
 * Test passing along various config settings that take objects other than strings as values.
 *
 * @author Steve Ebersole
 */
public class ConfigurationObjectSettingTest extends junit.framework.TestCase {
	public void testContainerBootstrapSharedCacheMode() {
		// first, via the integration vars
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		{
			// as object
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					empty,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}
		{
			// as string
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					empty,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE.name() )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}

		// next, via the PUI
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public SharedCacheMode getSharedCacheMode() {
				return SharedCacheMode.ENABLE_SELECTIVE;
			}
		};
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure( adapter, null );
			assertEquals( SharedCacheMode.ENABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}

		// via both, integration vars should take precedence
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					adapter,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}
	}

	public void testContainerBootstrapValidationMode() {
		// first, via the integration vars
		PersistenceUnitInfoAdapter empty = new PersistenceUnitInfoAdapter();
		{
			// as object
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					empty,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.CALLBACK )
			);
			assertEquals( ValidationMode.CALLBACK.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}
		{
			// as string
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					empty,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.CALLBACK.name() )
			);
			assertEquals( ValidationMode.CALLBACK.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}

		// next, via the PUI
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter() {
			@Override
			public ValidationMode getValidationMode() {
				return ValidationMode.CALLBACK;
			}
		};
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure( adapter, null );
			assertEquals( ValidationMode.CALLBACK.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}

		// via both, integration vars should take precedence
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					adapter,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.NONE )
			);
			assertEquals( ValidationMode.NONE.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}
	}

	public void testContainerBootstrapValidationFactory() {
		final Object token = new Object();
		PersistenceUnitInfoAdapter adapter = new PersistenceUnitInfoAdapter();
		Ejb3Configuration cfg = new Ejb3Configuration();
		try {
			cfg.configure(
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

	public void testStandaloneBootstrapSharedCacheMode() {
		// first, via the integration vars
		PersistenceMetadata metadata = new PersistenceMetadata();
		{
			// as object
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					metadata,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}
		{
			// as string
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					metadata,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE.name() )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}

		// next, via the PM
		metadata.setSharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE.name() );
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure( metadata, null );
			assertEquals( SharedCacheMode.ENABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}

		// via both, integration vars should take precedence
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					metadata,
					Collections.singletonMap( AvailableSettings.SHARED_CACHE_MODE, SharedCacheMode.DISABLE_SELECTIVE )
			);
			assertEquals( SharedCacheMode.DISABLE_SELECTIVE.name(), configured.getProperties().get( AvailableSettings.SHARED_CACHE_MODE ) );
		}
	}

	public void testStandaloneBootstrapValidationMode() {
		// first, via the integration vars
		PersistenceMetadata metadata = new PersistenceMetadata();
		{
			// as object
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					metadata,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.CALLBACK )
			);
			assertEquals( ValidationMode.CALLBACK.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}
		{
			// as string
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					metadata,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.CALLBACK.name() )
			);
			assertEquals( ValidationMode.CALLBACK.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}

		// next, via the PUI
		metadata.setValidationMode( ValidationMode.AUTO.name() );
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure( metadata, null );
			assertEquals( ValidationMode.AUTO.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}

		// via both, integration vars should take precedence
		{
			Ejb3Configuration cfg = new Ejb3Configuration();
			Ejb3Configuration configured = cfg.configure(
					metadata,
					Collections.singletonMap( AvailableSettings.VALIDATION_MODE, ValidationMode.NONE )
			);
			assertEquals( ValidationMode.NONE.name(), configured.getProperties().get( AvailableSettings.VALIDATION_MODE ) );
		}
	}

	public void testStandaloneBootstrapValidationFactory() {
		final Object token = new Object();
		PersistenceMetadata metadata = new PersistenceMetadata();
		Ejb3Configuration cfg = new Ejb3Configuration();
		try {
			cfg.configure(
					metadata,
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
