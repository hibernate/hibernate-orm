/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;
import static org.hibernate.cfg.JdbcSettings.SHOW_SQL;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-12271")
@BaseUnitTest
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportDropConstraints.class)
@ServiceRegistry(settings = {
		@Setting(name = HBM2DDL_AUTO, value = "none"),
		@Setting(name = FORMAT_SQL, value = "false"),
		@Setting(name = SHOW_SQL, value = "true")
})
@DomainModel(annotatedClasses = {
		ForeignKeyDropTest.ParentEntity.class,
		ForeignKeyDropTest.ChildEntity.class
})
public class ForeignKeyDropTest {
	@Test
	@JiraKey(value = "HHH-11236")
	public void testForeignKeyDropIsCorrectlyGenerated(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		final var scriptFile = new File( tmpDir, "script.sql" );

		final var schemaExport = new SchemaExport().setHaltOnError( false ).setOutputFile( scriptFile.getAbsolutePath() );
		schemaExport.drop( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), metadata );

		final Dialect dialect = registryScope.getRegistry().requireService( JdbcEnvironment.class ).getDialect();
		MatcherAssert.assertThat( "The ddl foreign key drop command has not been properly generated",
				checkDropForeignKeyConstraint( "CHILD_ENTITY", scriptFile, dialect ), is( true ) );
	}

	private boolean checkDropForeignKeyConstraint(
			String tableName,
			File scriptFile,
			Dialect dialect) throws IOException {
		boolean matches = false;
		String regex = dialect.getAlterTableString( tableName );
		regex += " " + dialect.getDropForeignKeyString() + " ";

		if ( dialect.supportsIfExistsBeforeConstraintName() ) {
			regex += "if exists ";
		}
		regex += "fk(.)*";
		if ( dialect.supportsIfExistsAfterConstraintName() ) {
			regex += " if exists";
		}

		return isMatching( matches, regex.toLowerCase(), scriptFile );
	}

	private boolean isMatching(boolean matches, String regex, File scriptFile) throws IOException {
		List<String> commands = Files.readAllLines( scriptFile.toPath() );

		Pattern p = Pattern.compile( regex );
		for ( String line : commands ) {
			final Matcher matcher = p.matcher( line.toLowerCase() );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}

	@SuppressWarnings("unused")
	@Entity(name = "ParentEntity")
	@Table(name = "PARENT_ENTITY")
	public static class ParentEntity {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "PARENT")
		Set<ChildEntity> children;
	}

	@SuppressWarnings("unused")
	@Entity(name = "ChildEntity")
	@Table(name = "CHILD_ENTITY")
	public static class ChildEntity {
		@Id
		private Long id;
	}
}
