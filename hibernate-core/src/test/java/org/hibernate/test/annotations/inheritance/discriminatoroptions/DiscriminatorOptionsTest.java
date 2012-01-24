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
package org.hibernate.test.annotations.inheritance.discriminatoroptions;

import org.junit.Test;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the @DiscriminatorOptions annotations.
 *
 * @author Hardy Ferentschik
 */
public class DiscriminatorOptionsTest extends BaseUnitTestCase {
	@Test
	public void testNonDefaultOptions() throws Exception {
		Configuration configuration = new Configuration();
		configuration.addAnnotatedClass( BaseClass.class );
		configuration.addAnnotatedClass( SubClass.class );
		configuration.buildMappings();
		PersistentClass persistentClass = configuration.getClassMapping( BaseClass.class.getName() );
		assertNotNull( persistentClass );
		assertTrue( persistentClass instanceof RootClass );

		RootClass root = ( RootClass ) persistentClass;
		assertTrue( "Discriminator should be forced", root.isForceDiscriminator() );
		assertFalse( "Discriminator should not be insertable", root.isDiscriminatorInsertable() );
	}

	@Test
	public void testBaseline() throws Exception {
		Configuration configuration = new Configuration()
				.addAnnotatedClass( BaseClass2.class )
				.addAnnotatedClass( SubClass2.class );
		configuration.buildMappings();
		PersistentClass persistentClass = configuration.getClassMapping( BaseClass2.class.getName() );
		assertNotNull( persistentClass );
		assertTrue( persistentClass instanceof RootClass );

		RootClass root = ( RootClass ) persistentClass;
		assertFalse( "Discriminator should not be forced by default", root.isForceDiscriminator() );
	}

	@Test
	public void testPropertyBasedDiscriminatorForcing() throws Exception {
		Configuration configuration = new Configuration()
				.setProperty( AvailableSettings.FORCE_DISCRIMINATOR_IN_SELECTS_BY_DEFAULT, "true" )
				.addAnnotatedClass( BaseClass2.class )
				.addAnnotatedClass( SubClass2.class );
		configuration.buildMappings();
		PersistentClass persistentClass = configuration.getClassMapping( BaseClass2.class.getName() );
		assertNotNull( persistentClass );
		assertTrue( persistentClass instanceof RootClass );

		RootClass root = ( RootClass ) persistentClass;
		assertTrue( "Discriminator should be forced by property", root.isForceDiscriminator() );
	}
}
