/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal;

import java.util.Iterator;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class MetadataImplTest extends BaseUnitTestCase {

	@Test(expected = IllegalArgumentException.class)
	public void testAddingNullClass() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addClass( null );
		sources.buildMetadata();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddingNullPackageName() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addPackage( null );
		sources.buildMetadata();
	}

	@Test(expected = HibernateException.class)
	public void testAddingNonExistingPackageName() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addPackage( "not.a.package" );
		sources.buildMetadata();
	}

	@Test
	public void testAddingPackageName() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addPackage( "org.hibernate.metamodel.source.internal" );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();

		assertFetchProfile( metadata );
	}

	@Test
	public void testAddingPackageNameWithTrailingDot() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		sources.addPackage( "org.hibernate.metamodel.source.internal." );
		MetadataImpl metadata = (MetadataImpl) sources.buildMetadata();

		assertFetchProfile( metadata );
	}

	@Test
	public void testGettingSessionFactoryBuilder() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		Metadata metadata = sources.buildMetadata();

		SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
		assertNotNull( sessionFactoryBuilder );
		assertTrue( SessionFactoryBuilderImpl.class.isInstance( sessionFactoryBuilder ) );

		SessionFactory sessionFactory = metadata.buildSessionFactory();
		assertNotNull( sessionFactory );
	}

	private void assertFetchProfile(MetadataImpl metadata) {
		Iterator<FetchProfile> profiles = metadata.getFetchProfiles().iterator();
		assertTrue( profiles.hasNext() );
		FetchProfile profile = profiles.next();
		assertEquals( "wrong profile name", "package-configured-profile", profile.getName() );
		assertFalse( profiles.hasNext() );
	}
}


