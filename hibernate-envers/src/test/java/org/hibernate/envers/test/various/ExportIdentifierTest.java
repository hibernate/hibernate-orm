/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.various;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
public class ExportIdentifierTest extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-12935" )
	public void testUniqueExportableIdentifier() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		final MetadataBuildingOptions options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( ssr );
		final Database database = new Database( options );

		database.locateNamespace( null, null );
		database.locateNamespace( Identifier.toIdentifier( "catalog1" ), null );
		database.locateNamespace( Identifier.toIdentifier( "catalog2" ), null );
		database.locateNamespace( null, Identifier.toIdentifier( "schema1" ) );
		database.locateNamespace( null, Identifier.toIdentifier( "schema2" ) );
		database.locateNamespace( Identifier.toIdentifier( "catalog_both_1" ), Identifier.toIdentifier( "schema_both_1" ) );
		database.locateNamespace( Identifier.toIdentifier( "catalog_both_2" ), Identifier.toIdentifier( "schema_both_2" ) );

		try {
			final Set<String> exportIdentifierSet = new HashSet<>();
			int namespaceSize = 0;
			for ( Namespace namespace : database.getNamespaces() ) {
				final SequenceStructure sequenceStructure = new SequenceStructure(
						ssr.getService( JdbcEnvironment.class ),
						new QualifiedNameImpl(
								namespace.getName(),
								Identifier.toIdentifier( "aSequence" )
						),
						1,
						1,
						Integer.class
				);
				sequenceStructure.registerExportables( database );
				exportIdentifierSet.add( namespace.getSequences().iterator().next().getExportIdentifier() );
				namespaceSize++;
			}
			assertEquals( 7, namespaceSize );
			assertEquals( 7, exportIdentifierSet.size() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
