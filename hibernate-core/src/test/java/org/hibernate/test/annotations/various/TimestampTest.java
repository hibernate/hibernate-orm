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
package org.hibernate.test.annotations.various;

import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.type.DbTimestampType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the @Timestamp annotation.
 *
 * @author Hardy Ferentschik
 */
public class TimestampTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testTimestampSourceIsVM() throws Exception {
		assertTimestampSource( VMTimestamped.class, TimestampType.class );
	}

	@Test
	public void testTimestampSourceIsDB() throws Exception {
		assertTimestampSource( DBTimestamped.class, DbTimestampType.class );
	}

	private void assertTimestampSource(Class<?> clazz, Class<?> expectedTypeClass) throws Exception {
		constructAndConfigureConfiguration();
		ClassMetadata meta = sessionFactory().getClassMetadata( clazz );
		assertTrue( "Entity is annotated with @Timestamp and should hence be versioned", meta.isVersioned() );

		EntityBinding binding = metadata().getEntityBinding( clazz.getName() );
		assertNotNull( binding );
		Type type = binding.getHierarchyDetails().getEntityVersion()
				.getVersioningAttributeBinding().getHibernateTypeDescriptor()
				.getResolvedTypeMapping();
		assertNotNull( type );
		assertEquals( "Wrong timestamp type", expectedTypeClass, type.getClass() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				VMTimestamped.class, DBTimestamped.class
		};
	}
}
