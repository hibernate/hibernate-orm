/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12106" )
@RequiresDialect( SQLServerDialect.class )
public class SqlServerQuoteSchemaTest extends BaseCoreFunctionalTestCase {

	private File output;

	@Override
	protected void afterSessionFactoryBuilt() {
		try {
			output = File.createTempFile( "update_script", ".sql" );
			output.deleteOnExit();
		}
		catch (IOException ignore) {
		}
		try {
			doInHibernate( this::sessionFactory, session -> {
				session.createNativeQuery(
					"DROP TABLE [my-schema].my_entity" )
				.executeUpdate();
			});
		}
		catch (Exception ignore) {
		}
		try {
			doInHibernate( this::sessionFactory, session -> {
				session.createNativeQuery(
					"DROP SCHEMA [my-schema]" )
				.executeUpdate();
			});
		}
		catch (Exception ignore) {
		}
		try {
			doInHibernate( this::sessionFactory, session -> {
				session.createNativeQuery(
					"CREATE SCHEMA [my-schema]" )
				.executeUpdate();
			});
		}
		catch (Exception ignore) {
		}
	}

	@Override
	protected void cleanupTest() {
		try {
			doInHibernate( this::sessionFactory, session -> {
				session.createNativeQuery(
						"DROP SCHEMA [my-schema]" )
						.executeUpdate();
			} );
		}
		catch (Exception ignore) {
		}
	}

	@Test
	public void test() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE.toString() )
				.build();

		try {
			output.deleteOnExit();

			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( MyEntity.class )
					.buildMetadata();
			metadata.validate();

			new SchemaUpdate()
					.setHaltOnError( true )
					.setOutputFile( output.getAbsolutePath() )
					.setDelimiter( ";" )
					.setFormat( true )
					.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}

		try {
			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			Pattern fileContentPattern = Pattern.compile( "create table \\[my\\-schema\\]\\.\\[my_entity\\]" );
			Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
			assertThat( fileContentMatcher.find(), is( true ) );
		}
		catch (IOException e) {
			fail(e.getMessage());
		}

		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE.toString() )
				.build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( MyEntityUpdated.class )
					.buildMetadata();
			metadata.validate();

			new SchemaUpdate()
					.setHaltOnError( true )
					.setOutputFile( output.getAbsolutePath() )
					.setDelimiter( ";" )
					.setFormat( true )
					.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}

		try {
			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			Pattern fileContentPattern = Pattern.compile( "alter table \\[my\\-schema\\]\\.\\[my_entity\\]" );
			Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
			assertThat( fileContentMatcher.find(), is( true ) );
		}
		catch (IOException e) {
			fail(e.getMessage());
		}
	}


	@Entity(name = "MyEntity")
	@Table(name = "my_entity", schema = "my-schema")
	public static class MyEntity {
		@Id
		public Integer id;
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity", schema = "my-schema")
	public static class MyEntityUpdated {
		@Id
		public Integer id;

		private String title;
	}

}
