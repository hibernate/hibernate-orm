/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.table;

import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.mapping.Table;
import org.hibernate.type.IntegerType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class Db2GenerationTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-9850" )
	public void testNewGeneratorTableCreationOnDb2() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, DB2Dialect.class.getName() )
				.build();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.buildMetadata();

			assertEquals( 0, metadata.getDatabase().getDefaultNamespace().getTables().size() );

			TableGenerator generator = new TableGenerator();

			Properties properties = new Properties();
			generator.configure( IntegerType.INSTANCE, properties, ssr );

			generator.registerExportables( metadata.getDatabase() );

			assertEquals( 1, metadata.getDatabase().getDefaultNamespace().getTables().size() );

			final Table table = metadata.getDatabase().getDefaultNamespace().getTables().iterator().next();
			final String[] createCommands = new DB2Dialect().getTableExporter().getSqlCreateStrings( table, metadata );
			assertContains( "sequence_name varchar(255) not null", createCommands[0] );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private void assertContains(String subStr, String str) {
		if ( !str.contains( subStr ) ) {
			Assert.fail( "String [" + str + "] did not contain expected substring [" + subStr + "]" );
		}
	}
	@Test
	@TestForIssue( jiraKey = "HHH-9850" )
	public void testLegacyGeneratorTableCreationOnDb2() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, DB2Dialect.class.getName() )
				.build();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.buildMetadata();

			assertEquals( 0, metadata.getDatabase().getDefaultNamespace().getTables().size() );

			MultipleHiLoPerTableGenerator generator = new MultipleHiLoPerTableGenerator();

			Properties properties = new Properties();
			generator.configure( IntegerType.INSTANCE, properties, ssr );

			generator.registerExportables( metadata.getDatabase() );

			assertEquals( 1, metadata.getDatabase().getDefaultNamespace().getTables().size() );

			final Table table = metadata.getDatabase().getDefaultNamespace().getTables().iterator().next();
			final String[] createCommands = new DB2Dialect().getTableExporter().getSqlCreateStrings( table, metadata );
			assertContains( "sequence_name varchar(255) not null", createCommands[0] );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
