/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.various;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.DbTimestampType;
import org.hibernate.type.TimestampType;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test for the @Timestamp annotation.
 *
 * @author Hardy Ferentschik
 */
public class TimestampTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeClassOnce
	public void setUp() {
		ssr = new StandardServiceRegistryBuilder().build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( VMTimestamped.class )
				.addAnnotatedClass( DBTimestamped.class )
				.getMetadataBuilder()
				.build();
	}

	@AfterClassOnce
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testTimestampSourceIsVM() throws Exception {
		assertTimestampSource( VMTimestamped.class, TimestampType.class );
	}

	@Test
	public void testTimestampSourceIsDB() throws Exception {
		assertTimestampSource( DBTimestamped.class, DbTimestampType.class );
	}

	private void assertTimestampSource(Class<?> clazz, Class<?> expectedTypeClass) throws Exception {
		PersistentClass persistentClass = metadata.getEntityBinding( clazz.getName() );
		assertNotNull( persistentClass );
		Property versionProperty = persistentClass.getVersion();
		assertNotNull( versionProperty );
		assertEquals( "Wrong timestamp type", expectedTypeClass, versionProperty.getType().getClass() );
	}
}
