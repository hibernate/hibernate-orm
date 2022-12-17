/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.generatedkeys.selectannotated;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Steve Ebersole
 * @author Marco Belladelli
 */
@DomainModel(
		annotatedClasses = MyEntity.class,
		xmlMappings = "org/hibernate/orm/test/generatedkeys/selectannotated/MyEntity.hbm.xml"
)
@SessionFactory
@RequiresDialect(value = OracleDialect.class)
public class SelectGeneratorTest {

	@Test
	public void testJDBC3GetGeneratedKeysSupportOnOracle(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity e = new MyEntity( "entity-1" );
					session.persist( e );

					// this insert should happen immediately!
					assertEquals( Long.valueOf( 1L ), e.getId(), "id not generated through forced insertion" );

					session.remove( e );
				}
		);
	}

	@Test
	@JiraKey("HHH-15900")
	public void testGeneratedKeyNotIdentityColumn(SessionFactoryScope scope) throws IOException {
		File output = File.createTempFile( "schema_export", ".sql" );
		output.deleteOnExit();

		final SchemaExport schemaExport = new SchemaExport();
		schemaExport.setOutputFile( output.getAbsolutePath() );
		schemaExport.execute(
				EnumSet.of( TargetType.SCRIPT ),
				SchemaExport.Action.CREATE,
				scope.getMetadataImplementor()
		);

		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertFalse( fileContent.toLowerCase().contains( "identity" ), "Column was generated as identity" );
	}
}
