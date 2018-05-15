/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.EnumSet;

import org.hibernate.annotations.Nationalized;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10529")
@RequiresDialect(value = SQLServer2008Dialect.class)
public class SQLServer2008NVarCharTypeTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;
	private SchemaExport schemaExport;

	@Before
	public void setUp() {
		ssr = new StandardServiceRegistryBuilder().build();
		schemaExport = createSchemaExport( new Class[] {MyEntity.class} );
	}

	@After
	public void tearDown() {
		schemaExport.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSchemaIsCreatedWithoutExceptions() {
		schemaExport.createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	private SchemaExport createSchemaExport(Class[] annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
		SchemaExport schemaExport = new SchemaExport();
		schemaExport.setHaltOnError( true )
				.setFormat( false );

		return schemaExport;
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		long id;

		@Nationalized
		@Column(length = 4001)
		String name;
	}
}
