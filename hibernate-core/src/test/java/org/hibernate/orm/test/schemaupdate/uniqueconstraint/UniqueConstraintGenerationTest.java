/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import org.hamcrest.MatcherAssert;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(xmlMappings = "org/hibernate/orm/test/schemaupdate/uniqueconstraint/TestEntity.hbm.xml")
public class UniqueConstraintGenerationTest {

	@Test
	@JiraKey(value = "HHH-11101")
	public void testUniqueConstraintIsGenerated(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "update_script.sql" );

		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaExport()
				.setOutputFile( scriptFile.getAbsolutePath() )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		final var dialect = registryScope.getRegistry().requireService( JdbcEnvironment.class ).getDialect();
		if ( !(dialect.getUniqueDelegate() instanceof SkipNullableUniqueDelegate) ) {
			if ( dialect.getUniqueDelegate() instanceof AlterTableUniqueIndexDelegate ) {
				MatcherAssert.assertThat( "The test_entity_item table unique constraint has not been generated",
						isCreateUniqueIndexGenerated("test_entity_item", "item", scriptFile),
						is(true)
				);
			}
			else {
				MatcherAssert.assertThat( "The test_entity_item table unique constraint has not been generated",
						isUniqueConstraintGenerated("test_entity_item", "item", dialect, scriptFile),
						is(true)
				);
			}

			MatcherAssert.assertThat( "The test_entity_children table unique constraint has not been generated",
					isUniqueConstraintGenerated( "test_entity_children", "child", dialect, scriptFile ),
					is( true )
			);
		}
	}

	private boolean isUniqueConstraintGenerated(
			String tableName,
			String columnName,
			Dialect dialect,
			File scriptFile) throws IOException {
		final String regex;
		if ( dialect.getUniqueDelegate() instanceof CreateTableUniqueDelegate ) {
			regex = dialect.getCreateTableString() + " " + tableName + " .* " + columnName + " .+ unique.*\\)"
					+ dialect.getTableTypeString().toLowerCase() + ";";
		}
		else if ( dialect.getUniqueDelegate() instanceof AlterTableUniqueDelegate) {
			regex = dialect.getAlterTableString( tableName ) + " add constraint uk.* unique \\(" + columnName + "\\);";
		}
		else {
			return true;
		}

		final String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) ).toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		for ( String line : split ) {
			if ( line.matches(regex) ) {
				return true;
			}
		}
		return false;
	}

	private boolean isCreateUniqueIndexGenerated(
			String tableName,
			String columnName,
			File scriptFile) throws IOException {
		String regex = "create unique (nonclustered )?index uk.* on " + tableName
				+ " \\(" + columnName + "\\)( where .*| exclude null keys)?;";
		final String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) ).toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		Pattern p = Pattern.compile( regex );
		for ( String line : split ) {
			final Matcher matcher = p.matcher( line );
			if ( matcher.matches() ) {
				return true;
			}
		}
		return false;
	}
}
