/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.TestForIssue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Originally written to verify fix for HHH-10172
 *
 * @author Steve Ebersole
 */
public class GetAndIsVariantGetterTest {
	private static StandardServiceRegistry ssr;

	@BeforeClass
	public static void prepare() {
		ssr = new StandardServiceRegistryBuilder(  ).build();
	}

	@AfterClass
	public static void release() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10172" )
	public void testHbmXml() {
		try {
			new MetadataSources( ssr )
					.addResource( "org/hibernate/property/TheEntity.hbm.xml" )
					.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (MappingException ignore) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10172" )
	public void testAnnotations() {
		new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity.class )
				.buildMetadata();
	}

	@Entity
	public static class TheEntity {
		private Integer id;

		public boolean isId() {
			return false;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
