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
package org.hibernate.metamodel.internal.source.annotations.global;

import java.util.Iterator;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class FetchProfileBinderTest extends BaseUnitTestCase {
	private StandardServiceRegistryImpl serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	public void testSingleFetchProfile() {
		@FetchProfile(name = "foo", fetchOverrides = {
				@FetchProfile.FetchOverride(entity = Foo.class, association = "bars", mode = FetchMode.JOIN)
		})
		@Entity
		class Foo {
			@Id
			@GeneratedValue
			public long id;
			
			@ElementCollection
			public List<String> bars;
		}

		Metadata meta = new MetadataSources( serviceRegistry ).addAnnotatedClass( Foo.class ).buildMetadata();

		Iterator<org.hibernate.metamodel.spi.binding.FetchProfile> mappedFetchProfiles = meta.getFetchProfiles().iterator();
		assertTrue( mappedFetchProfiles.hasNext() );
		org.hibernate.metamodel.spi.binding.FetchProfile profile = mappedFetchProfiles.next();
		assertEquals( "Wrong fetch profile name", "foo", profile.getName() );
		org.hibernate.metamodel.spi.binding.FetchProfile.Fetch fetch = profile.getFetches().iterator().next();
		assertEquals( "Wrong association name", "bars", fetch.getAssociation() );
		assertEquals( "Wrong association type", Foo.class.getName(), fetch.getEntity() );
	}

	@Test
	public void testFetchProfiles() {
		Metadata meta = new MetadataSources( serviceRegistry ).addAnnotatedClass( FooBar.class ).buildMetadata();

		Iterator<org.hibernate.metamodel.spi.binding.FetchProfile> mappedFetchProfiles = meta.getFetchProfiles().iterator();
		assertTrue( mappedFetchProfiles.hasNext() );
		org.hibernate.metamodel.spi.binding.FetchProfile profile = mappedFetchProfiles.next();
		assertProfiles( profile );

		assertTrue( mappedFetchProfiles.hasNext() );
		profile = mappedFetchProfiles.next();
		assertProfiles( profile );
	}

	private void assertProfiles(org.hibernate.metamodel.spi.binding.FetchProfile profile) {
		if ( profile.getName().equals( "foobar" ) ) {
			org.hibernate.metamodel.spi.binding.FetchProfile.Fetch fetch = profile.getFetches().iterator().next();
			assertEquals( "Wrong association name", "fubars", fetch.getAssociation() );
			assertEquals( "Wrong association type", FooBar.class.getName(), fetch.getEntity() );
		}
		else if ( profile.getName().equals( "fubar" ) ) {
			org.hibernate.metamodel.spi.binding.FetchProfile.Fetch fetch = profile.getFetches().iterator().next();
			assertEquals( "Wrong association name", "fubars", fetch.getAssociation() );
			assertEquals( "Wrong association type", FooBar.class.getName(), fetch.getEntity() );
		}
		else {
			fail( "Wrong fetch name:" + profile.getName() );
		}
	}

	@Test(expected = MappingException.class)
	public void testNonJoinFetchThrowsException() {
		@FetchProfile(name = "foo", fetchOverrides = {
				@FetchProfile.FetchOverride(entity = Foo.class, association = "bars", mode = FetchMode.SELECT)
		})
		@Entity
		class Foo {
			@Id
			@GeneratedValue
			public long id;
			
			@ElementCollection
			public List<String> bars;
		}

		new MetadataSources( serviceRegistry ).addAnnotatedClass( Foo.class ).buildMetadata();
	}

	@FetchProfiles( {
			@FetchProfile(name = "foobar", fetchOverrides = {
					@FetchProfile.FetchOverride(entity = FooBar.class, association = "fubars", mode = FetchMode.JOIN)
			}),
			@FetchProfile(name = "fubar", fetchOverrides = {
					@FetchProfile.FetchOverride(entity = FooBar.class, association = "fubars", mode = FetchMode.JOIN)
			})
	})
	@Entity
	class FooBar {
		@Id
		@GeneratedValue
		public long id;
		
		@ElementCollection
		public List<String> fubars;
	}
}


