/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.idgenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(annotatedClasses = TableGeneratorsTest.TestEntity.class)
public class TableGeneratorsTest {
	private static final int INITIAL_VALUE = 5;
	private static final int EXPECTED_DB_INSERTED_VALUE = INITIAL_VALUE;

	@Test
	public void testTableGeneratorIsGenerated(DomainModelScope modelScope, @TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( true );
		metadata.validate();

		new SchemaExport()
				.setOutputFile( scriptFile.getAbsolutePath() )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		final List<String> commands = Files.readAllLines( scriptFile.toPath() );

		final String expectedTestEntityTableCreationCommand = "CREATE TABLE TEST_ENTITY \\(ID .*, PRIMARY KEY \\(ID\\)\\);";
		Assertions.assertTrue( isCommandGenerated( commands, expectedTestEntityTableCreationCommand ),
				"The command '" + expectedTestEntityTableCreationCommand + "' has not been correctly generated" );

		final String expectedIdTableGeneratorCreationCommand = "CREATE TABLE ID_TABLE_GENERATOR \\(VALUE .*, PK .*, PRIMARY KEY \\(PK\\)\\);";
		Assertions.assertTrue( isCommandGenerated(
				commands,
				expectedIdTableGeneratorCreationCommand
		), "The command '" + expectedIdTableGeneratorCreationCommand + "' has not been correctly generated" );

		final String expectedInsertIntoTableGeneratorCommand = "INSERT INTO ID_TABLE_GENERATOR\\(PK, VALUE\\) VALUES \\('TEST_ENTITY_ID'," + EXPECTED_DB_INSERTED_VALUE + "\\);";
		Assertions.assertTrue( isCommandGenerated(
				commands,
				expectedInsertIntoTableGeneratorCommand
		), "The command '" + expectedInsertIntoTableGeneratorCommand + "' has not been correctly generated" );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	@TableGenerator(name = "tableGenerator",
			table = "ID_TABLE_GENERATOR",
			pkColumnName = "PK",
			pkColumnValue = "TEST_ENTITY_ID",
			valueColumnName = "VALUE",
			allocationSize = 3,
			initialValue = INITIAL_VALUE
	)
	public static class TestEntity {
		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "tableGenerator")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	private boolean isCommandGenerated(List<String> commands, String expectedCommnad) {
		final Pattern pattern = Pattern.compile( expectedCommnad.toLowerCase() );
		for ( String command : commands ) {
			Matcher matcher = pattern.matcher( command.toLowerCase() );
			if ( matcher.matches() ) {
				return true;
			}
		}
		return false;
	}
}
