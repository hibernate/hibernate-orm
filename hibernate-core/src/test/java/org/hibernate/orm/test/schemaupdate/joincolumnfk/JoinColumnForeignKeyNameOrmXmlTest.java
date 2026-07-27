/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.joincolumnfk;

import org.hamcrest.MatcherAssert;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-20725")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
public class JoinColumnForeignKeyNameOrmXmlTest {

	@Test
	@DomainModel(xmlMappings = {
			"org/hibernate/orm/test/schemaupdate/joincolumnfk/Department.orm.xml",
			"org/hibernate/orm/test/schemaupdate/joincolumnfk/Worker.orm.xml"
	})
	public void testManyToOneForeignKeyNameFromJoinColumn(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ), metadata );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		MatcherAssert.assertThat( fileContent.toLowerCase().contains( "fk_worker_dept" ), is( true ) );
	}

	@Test
	@DomainModel(xmlMappings = {
			"org/hibernate/orm/test/schemaupdate/joincolumnfk/Division.orm.xml",
			"org/hibernate/orm/test/schemaupdate/joincolumnfk/VicePresident.orm.xml",
			"org/hibernate/orm/test/schemaupdate/joincolumnfk/Company.orm.xml"
	})
	public void testMapKeyJoinColumnForeignKeyNameFromOrmXml(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ), metadata );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		MatcherAssert.assertThat( fileContent.toLowerCase().contains( "fk_org_div" ), is( true ) );
	}
}
