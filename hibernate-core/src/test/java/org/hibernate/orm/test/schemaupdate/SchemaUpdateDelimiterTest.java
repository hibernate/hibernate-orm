/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-1122")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(annotatedClasses = SchemaUpdateDelimiterTest.TestEntity.class)
public class SchemaUpdateDelimiterTest {
	public static final String EXPECTED_DELIMITER = ";";

	@Test
	public void testSchemaUpdateApplyDelimiterToGeneratedSQL(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var output = new File( tmpDir, "update_script.sql" );

		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setDelimiter( EXPECTED_DELIMITER )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.SCRIPT ), model );

		var sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
		for ( var line : sqlLines ) {
			MatcherAssert.assertThat( "The expected delimiter is not applied " + line,
					line.endsWith( EXPECTED_DELIMITER ), is( true ) );
		}
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
