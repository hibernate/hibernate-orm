/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.MatcherAssert;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = SchemaMigrationTargetScriptCreationTest.TestEntity.class)
@SessionFactory(exportSchema = false)
public class SchemaMigrationTargetScriptCreationTest implements ServiceRegistryProducer {
	private final File output;

	public SchemaMigrationTargetScriptCreationTest(@TempDir File outputDir) {
		this.output = new File( outputDir, "update_script.sql" );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.applySetting( JAKARTA_HBM2DDL_DATABASE_ACTION, "update" )
				.applySetting( JAKARTA_HBM2DDL_SCRIPTS_ACTION, "update" )
				.applySetting( JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, output.getAbsolutePath() )
				.build();
	}

	@BeforeEach
	void setUp(DomainModelScope modelScope) {
		// for whatever reason, on CI, these tables sometimes exist (sigh)
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
	}

	@Test
	@JiraKey(value = "HHH-10684")
	public void testTargetScriptIsCreated(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.getSessionFactory();
		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		MatcherAssert.assertThat( "Script file : " + fileContent.toLowerCase(), fileContentMatcher.find(), is( true ) );
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
