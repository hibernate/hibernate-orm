/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "10180")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(annotatedClasses = SchemaUpdateGeneratingOnlyScriptFileTest.TestEntity.class)
public class SchemaUpdateGeneratingOnlyScriptFileTest {

	@Test
	public void testSchemaUpdateScriptGeneration(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var output = new File( tmpDir, "update_script.sql" );

		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ), model );

		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertTrue( fileContentMatcher.find() );
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}
}
