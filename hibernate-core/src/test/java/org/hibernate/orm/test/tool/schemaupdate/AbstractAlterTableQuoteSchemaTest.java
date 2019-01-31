/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.Helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractAlterTableQuoteSchemaTest extends SessionFactoryBasedFunctionalTest {

	protected File output;

	@Override
	protected boolean exportSchema() {
		return false;
	}

	protected void setUp(String schemaName) throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		try {
			inTransaction(
					session ->
							session.doWork( work -> {
								work.prepareStatement( "DROP TABLE " + quote( schemaName, "my_entity" ) ).execute();
								work.commit();
							} )
			);
		}
		catch (Exception ignore) {
		}
		try {
			inTransaction(
					session ->
							session.doWork( work -> {
								work.prepareStatement( "DROP SCHEMA " + quote( schemaName ) ).execute();
								work.commit();
							} )
			);
		}
		catch (Exception ignore) {
		}
		inTransaction(
				session ->
						session.doWork( work -> {
							work.prepareStatement( "CREATE SCHEMA " + quote( schemaName ) ).execute();
							work.commit();
						} )
		);
	}

	protected void tearDown(String schemaName) {
		try {
			inTransaction(
					session ->
							session.doWork( work -> {
								work.prepareStatement( "DROP SCHEMA " + quote( schemaName ) ).execute();
								work.commit();
							} )
			);
		}
		catch (Exception ignore) {
		}
	}

	protected void executeSchemaUpdate(Class... annotatedClasses) {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE.toString() )
				.build();
		try {
			new SchemaUpdate( getDatabaseModel( ssr, annotatedClasses ), ssr )
					.setHaltOnError( true )
					.setOutputFile( output.getAbsolutePath() )
					.setDelimiter( ";" )
					.setFormat( true )
					.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	protected void assertThatSqlCommandIsExecuted(String createTableCommand) throws Exception {
		String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		Pattern fileContentPattern = Pattern
				.compile( createTableCommand );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat( fileContentMatcher.find(), is( true ) );
	}

	private DatabaseModel getDatabaseModel(StandardServiceRegistry ssr, Class... annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr ) {
			@Override
			public MetadataBuilder getMetadataBuilder() {
				MetadataBuilder metadataBuilder = super.getMetadataBuilder();
				metadataBuilder.applyImplicitSchemaName( "default-schema" );
				return metadataBuilder;
			}
		};
		for ( int i = 0; i < annotatedClasses.length; i++ ) {
			metadataSources.addAnnotatedClass( annotatedClasses[i] );
		}

		final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		return Helper.buildDatabaseModel( ssr, metadata );
	}

	protected String quote(String element) {
		return getDialect().quote( "`" + element + "`" );
	}

	protected String quote(String schema, String table) {
		return quote( schema ) + "." + quote( table );
	}

	protected String regexpQuote(String element) {
		return getDialect().quote( "`" + element + "`" )
				.replace( "-", "\\-" )
				.replace( "[", "\\[" )
				.replace( "]", "\\]" );
	}

	protected String regexpQuote(String schema, String table) {
		return regexpQuote( schema ) + "\\." + regexpQuote( table );
	}

	private Dialect getDialect() {
		return sessionFactory().getDialect();
	}
}
