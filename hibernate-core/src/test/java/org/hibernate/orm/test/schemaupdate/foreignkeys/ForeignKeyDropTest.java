/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-12271")
@RequiresDialectFeature(DialectChecks.SupportDropConstraints.class)
public class ForeignKeyDropTest extends BaseUnitTestCase {
	private File output;
	private MetadataImplementor metadata;
	private StandardServiceRegistry ssr;
	private SchemaExport schemaExport;

	@Before
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.HBM2DDL_AUTO, "none" )
				.applySetting( Environment.FORMAT_SQL, "false" )
				.applySetting( Environment.SHOW_SQL, "true" )
				.build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( ParentEntity.class )
				.addAnnotatedClass( ChildEntity.class )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		schemaExport = new SchemaExport().setHaltOnError( false ).setOutputFile( output.getAbsolutePath() );
	}

	@Test
	@JiraKey(value = "HHH-11236")
	public void testForeignKeyDropIsCorrectlyGenerated() throws Exception {

		schemaExport
				.drop( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), metadata );

		assertThat(
				"The ddl foreign key drop command has not been properly generated",
				checkDropForeignKeyConstraint( "CHILD_ENTITY" ),
				is( true )
		);
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	protected Dialect getDialect() {
		return ssr.getService( JdbcEnvironment.class ).getDialect();
	}

	private boolean checkDropForeignKeyConstraint(String tableName) throws IOException {
		boolean matches = false;
		String regex = getDialect().getAlterTableString( tableName );
		regex += " " + getDialect().getDropForeignKeyString() + " ";

		if ( getDialect().supportsIfExistsBeforeConstraintName() ) {
			regex += "if exists ";
		}
		regex += "fk(.)*";
		if ( getDialect().supportsIfExistsAfterConstraintName() ) {
			regex += " if exists";
		}

		return isMatching( matches, regex.toLowerCase() );
	}

	private boolean isMatching(boolean matches, String regex) throws IOException {
		List<String> commands = Files.readAllLines( output.toPath() );

		Pattern p = Pattern.compile( regex );
		for ( String line : commands ) {
			final Matcher matcher = p.matcher( line.toLowerCase() );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}

	@Entity(name = "ParentEntity")
	@Table(name = "PARENT_ENTITY")
	public static class ParentEntity {
		@Id
		private Long id;

		@OneToMany
		@JoinColumn(name = "PARENT")
		Set<ChildEntity> children;
	}

	@Entity(name = "ChildEntity")
	@Table(name = "CHILD_ENTITY")
	public static class ChildEntity {
		@Id
		private Long id;
	}
}
