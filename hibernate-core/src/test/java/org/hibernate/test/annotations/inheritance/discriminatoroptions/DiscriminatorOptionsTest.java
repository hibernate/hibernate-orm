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

//$Id$
package org.hibernate.test.annotations.inheritance.discriminatoroptions;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.test.annotations.TestCase;

/**
 * Test for the @DiscriminatorOptions annotations.
 *
 * @author Hardy Ferentschik
 */
public class DiscriminatorOptionsTest extends TestCase {

	public void testNonDefaultOptions() throws Exception {
		buildConfiguration();

		PersistentClass persistentClass = cfg.getClassMapping( BaseClass.class.getName() );
		assertNotNull( persistentClass );
		assertTrue( persistentClass instanceof RootClass );

		RootClass root = ( RootClass ) persistentClass;
		assertTrue( "Discriminator should be forced", root.isForceDiscriminator() );
		assertFalse( "Discriminator should not be insertable", root.isDiscriminatorInsertable() );
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				BaseClass.class, SubClass.class
		};
	}
}
