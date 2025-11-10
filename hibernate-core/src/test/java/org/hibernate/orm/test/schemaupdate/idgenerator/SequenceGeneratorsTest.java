/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.idgenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SequenceGenerators;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(annotatedClasses = SequenceGeneratorsTest.TestEntity.class)
public class SequenceGeneratorsTest {
	@Test
	public void testSequenceIsGenerated(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaExport()
				.setOutputFile( scriptFile.getAbsolutePath() )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		var commands = Files.readAllLines( scriptFile.toPath() );

		MatcherAssert.assertThat(
				isCommandGenerated( commands, "CREATE TABLE TEST_ENTITY \\(ID .*, PRIMARY KEY \\(ID\\)\\);" ),
				is( true ) );

		MatcherAssert.assertThat(
				isCommandGenerated( commands, "CREATE SEQUENCE SEQUENCE_GENERATOR START WITH 5 INCREMENT BY 3;" ),
				is( true ) );
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


	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQUENCEGENERATOR")
		@SequenceGenerators({
				@SequenceGenerator(
						name = "SEQUENCEGENERATOR",
						allocationSize = 3,
						initialValue = 5,
						sequenceName = "SEQUENCE_GENERATOR")
		})
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
