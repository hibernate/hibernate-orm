/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.hbm2ddl.Target;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-9693" )
public class LongVarcharValidationTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void beforeTest() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void afterTest() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testValidation() {
		MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( Translation.class )
				.buildMetadata();
		metadata.validate();


		// create the schema
		new SchemaExport( metadata ).create( Target.EXPORT );

		try {
			new SchemaValidator( metadata ).validate();
		}
		finally {
			new SchemaExport( metadata ).drop( Target.EXPORT );
		}
	}

	@Entity(name = "Translation")
	public static class Translation {
		@Id
		public Integer id;
		@Type( type = "text" )
		String text;
	}

}
