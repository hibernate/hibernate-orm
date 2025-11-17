/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Pattern;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Koen Aers
 */
@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-10158")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(annotatedClasses = SchemaUpdateFormatterTest.TestEntity.class)
public class SchemaUpdateFormatterTest {

	private static final String AFTER_FORMAT =
			"\n\\s+create table test_entity \\(\n" +
			"\\s+field varchar\\(255\\) not null,\n" +
			"\\s+primary key \\(field\\)\n" +
			"\\s+\\).*?;\n";
	private static final String DELIMITER = ";";

	@Test
	public void testSetFormat(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var output = new File( tmpDir, "update_script.sql" );

		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setDelimiter( DELIMITER )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ), model );

		String outputContent = new String(Files.readAllBytes(output.toPath()));
		//Old Macs use \r as a new line delimiter
		outputContent = outputContent.replaceAll( "\r", "\n");
		//On Windows, \r\n would become \n\n, so we eliminate duplicates
		outputContent = outputContent.replaceAll( "\n\n", "\n");

		Assertions.assertTrue( Pattern.compile( AFTER_FORMAT ).matcher( outputContent ).matches() );
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
