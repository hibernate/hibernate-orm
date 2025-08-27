/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generatedkeys.generated;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.hibernate.community.dialect.TiDBDialect;
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
@RequiresDialect(OracleDialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(H2Dialect.class)
@RequiresDialect(DB2Dialect.class)
@RequiresDialect(SQLServerDialect.class)
@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB does not support triggers")
public class GeneratedTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity e1 = new MyEntity( "entity-1" );
					session.persist( e1 );
					// this insert should happen immediately!
					assertEquals( Long.valueOf( 1L ), e1.getId(), "id not generated through forced insertion" );

					MyEntity e2 = new MyEntity( "entity-2" );
					session.persist( e2 );
					assertEquals( Long.valueOf( 2L ), e2.getId(), "id not generated through forced insertion" );

					session.remove( e1 );
					session.remove( e2 );
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
