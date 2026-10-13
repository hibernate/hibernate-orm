package org.hibernate.orm.test.schema.guide;

import java.io.StringWriter;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Map;

import jakarta.persistence.Persistence;
import jakarta.persistence.SchemaManager;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator.ActionGrouping;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.SchemaToolingSettings.*;

/// Executable examples for the Schema Generation chapter.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class SchemaGuideTests {
	@TempDir
	Path directory;

	//tag::schema-guide-configuration[]
	private static HibernatePersistenceConfiguration configuration(String url) {
		var configuration = new HibernatePersistenceConfiguration( "SchemaGuide" )
				.managedClasses( SchemaGuideBook.class )
				.jdbcUrl( url + ";DB_CLOSE_DELAY=-1" )
				.jdbcUsername( "sa" )
				.jdbcPassword( "" );
		//tag::schema-guide-assertions[]
		// Avoid a competing Hibernate URL alias from the test environment.
		configuration.property( JdbcSettings.URL, url + ";DB_CLOSE_DELAY=-1" );
		//end::schema-guide-assertions[]
		return configuration;
	}
	//end::schema-guide-configuration[]

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void scriptsOnlyDoesNotCreateTables(boolean withoutConnection) throws Exception {
		var configuration = configuration( "jdbc:h2:mem:schema_scripts" );
		if ( withoutConnection ) {
			// Supplying the Dialect and disabling metadata access permits offline script generation.
			configuration.property( JdbcSettings.DIALECT, H2Dialect.class.getName() )
					.property( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
					.property( JdbcSettings.CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() );
		}
		//tag::schema-guide-scripts[]
		var createScript = new StringWriter();
		var dropScript = new StringWriter();
		configuration.property( "jakarta.persistence.schema-generation.database.action", "none" )
				.property( "jakarta.persistence.schema-generation.scripts.action", "drop-and-create" )
				.property( "jakarta.persistence.schema-generation.scripts.create-target", createScript )
				.property( "jakarta.persistence.schema-generation.scripts.drop-target", dropScript );
		Persistence.generateSchema( configuration );
		//end::schema-guide-scripts[]
		assertThat( createScript.toString().toLowerCase() ).contains( "create table schema_book", "positive_edition",
				"uk_schema_book_title_author", "idx_schema_book_author", "generated always as" );
		assertThat( dropScript.toString().toLowerCase() ).contains( "drop table" );
		assertThat( exists( "jdbc:h2:mem:schema_scripts", "SCHEMA_BOOK" ) ).isFalse();
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "true,false", "false,true", "true,true", "false,", "true," })
	void scriptFilesRespectAppendSetting(boolean fileUrl, Boolean append) throws Exception {
		var target = directory.resolve( "create script.sql" );
		var dropTarget = directory.resolve( "drop script.sql" );
		var configuration = configuration( "jdbc:h2:mem:schema_script_file" );
		configuration.property( JAKARTA_HBM2DDL_DATABASE_ACTION, "none" )
				.property( JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" )
				.property( JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, fileUrl ? target.toUri().toString() : target.toString() )
				.property( JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, fileUrl ? dropTarget.toUri().toString() : dropTarget.toString() );
		if ( append != null ) {
			configuration.property( HBM2DDL_SCRIPTS_CREATE_APPEND, append );
		}
		Persistence.generateSchema( configuration );
		var first = Files.readString( target );
		var firstDrop = Files.readString( dropTarget );
		assertThat( first ).contains( "create table schema_book" );
		assertThat( firstDrop ).contains( "drop table" );
		Persistence.generateSchema( configuration );
		boolean shouldAppend = append == null || append;
		assertThat( Files.readString( target ) ).isEqualTo( shouldAppend ? first + first : first );
		assertThat( Files.readString( dropTarget ) ).isEqualTo( shouldAppend ? firstDrop + firstDrop : firstDrop );
	}

	@Test
	void alterationScriptDoesNotChangeExistingDatabase() throws Exception {
		var url = "jdbc:h2:mem:schema_alterations";
		try (var connection = DriverManager.getConnection( url + ";DB_CLOSE_DELAY=-1", "sa", "" );
				var statement = connection.createStatement()) {
			statement.executeUpdate( "create table schema_book (id bigint primary key, title varchar(100) not null, author varchar(100) not null)" );
			statement.executeUpdate( "insert into schema_book (id, title, author) values (1, 'Existing book', 'Hibernate')" );
		}
		try {
			//tag::schema-guide-alterations[]
			var configuration = configuration( "jdbc:h2:mem:schema_alterations" );
			var alterationScript = new StringWriter();
			configuration.property( "jakarta.persistence.schema-generation.database.action", "none" )
					.property( "jakarta.persistence.schema-generation.scripts.action", "update" )
					.property( "jakarta.persistence.schema-generation.scripts.create-target", alterationScript );
			Persistence.generateSchema( configuration );
			//end::schema-guide-alterations[]
			assertThat( alterationScript.toString().replaceAll( "\\s+", " " ).toLowerCase() )
					.containsPattern( "alter table(?: if exists)? schema_book add column edition" );
			try (var connection = DriverManager.getConnection( url, "sa", "" );
					var columns = connection.getMetaData().getColumns( null, null, "SCHEMA_BOOK", "EDITION" );
					var statement = connection.createStatement();
					var rows = statement.executeQuery( "select id, title, author from schema_book" )) {
				assertThat( columns.next() ).isFalse();
				assertThat( rows.next() ).isTrue();
				assertThat( rows.getLong( "id" ) ).isEqualTo( 1L );
				assertThat( rows.getString( "title" ) ).isEqualTo( "Existing book" );
				assertThat( rows.getString( "author" ) ).isEqualTo( "Hibernate" );
				assertThat( rows.next() ).isFalse();
			}
		}
		finally {
			try (var connection = DriverManager.getConnection( url, "sa", "" );
					var statement = connection.createStatement()) {
				statement.executeUpdate( "drop table schema_book" );
			}
		}
	}

	@Test
	void metadataAndScriptsCreateObjectsAndLoadData() throws Exception {
		var configuration = configuration( "jdbc:h2:mem:schema_sources" );
		//tag::schema-guide-sources[]
		configuration.property( "jakarta.persistence.schema-generation.database.action", "drop-and-create" )
				.property( "jakarta.persistence.schema-generation.create-source", "metadata-then-script" )
				.property( "jakarta.persistence.schema-generation.create-script-source", getClass().getResource( "extra-create.sql" ).toExternalForm() )
				.property( "jakarta.persistence.schema-generation.drop-source", "script-then-metadata" )
				.property( "jakarta.persistence.schema-generation.drop-script-source", getClass().getResource( "extra-drop.sql" ).toExternalForm() )
				.property( "jakarta.persistence.sql-load-script-source", getClass().getResource( "load.sql" ).toExternalForm() )
				.property( "hibernate.hbm2ddl.halt_on_error", true );
		Persistence.generateSchema( configuration );
		//end::schema-guide-sources[]
		assertThat( exists( "jdbc:h2:mem:schema_sources", "SCHEMA_NOTE" ) ).isTrue();
		configuration.property( JAKARTA_HBM2DDL_DATABASE_ACTION, "none" );
		try (var factory = configuration.createEntityManagerFactory()) {
			try (var entityManager = factory.createEntityManager()) {
				var book = entityManager.find( SchemaGuideBook.class, 1L );
				assertThat( book.title ).isEqualTo( "Hibernate User Guide" );
				assertThat( book.edition ).isEqualTo( 1 );
			}
			factory.getSchemaManager().drop( false );
		}
		assertThat( exists( "jdbc:h2:mem:schema_sources", "SCHEMA_NOTE" ) ).isFalse();
	}

	@Test
	void schemaManagerOperations() throws Exception {
		var configuration = configuration( "jdbc:h2:mem:schema_manager" );
		configuration.property( JAKARTA_HBM2DDL_DATABASE_ACTION, "none" )
				.property( JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, getClass().getResource( "load.sql" ).toExternalForm() );
		try (var factory = configuration.createEntityManagerFactory()) {
			//tag::schema-guide-manager[]
			SchemaManager schemaManager = factory.getSchemaManager();
			schemaManager.create( false );
			schemaManager.validate();
			//end::schema-guide-manager[]
			try (var entityManager = factory.createEntityManager()) {
				assertThat( entityManager.find( SchemaGuideBook.class, 1L ) ).isNotNull();
			}
			//tag::schema-guide-populate[]
			schemaManager.truncate();
			//end::schema-guide-populate[]
			try (var entityManager = factory.createEntityManager()) {
				assertThat( entityManager.createQuery( "select count(b) from SchemaGuideBook b", Long.class ).getSingleResult() ).isEqualTo( 1L );
			}
			// Delete the initial row so explicit population can import it again.
			try (var entityManager = factory.createEntityManager()) {
				entityManager.getTransaction().begin();
				entityManager.unwrap( org.hibernate.Session.class ).createMutationQuery( "delete from SchemaGuideBook" ).executeUpdate();
				entityManager.getTransaction().commit();
			}
			schemaManager.populate();
			try (var entityManager = factory.createEntityManager()) {
				assertThat( entityManager.find( SchemaGuideBook.class, 1L ) ).isNotNull();
			}
			//tag::schema-guide-drop[]
			schemaManager.drop( false );
			//end::schema-guide-drop[]
		}
		assertThat( exists( "jdbc:h2:mem:schema_manager", "SCHEMA_BOOK" ) ).isFalse();
	}

	@Test
	void generatedValuesAndConstraints() {
		var configuration = configuration( "jdbc:h2:mem:schema_generated" );
		configuration.property( JAKARTA_HBM2DDL_DATABASE_ACTION, "drop-and-create" );
		try (var factory = configuration.createEntityManagerFactory()) {
			//tag::schema-guide-generated[]
			try (var entityManager = factory.createEntityManager()) {
				entityManager.getTransaction().begin();
				var book = new SchemaGuideBook( 1L, "Hibernate", "Hibernate" );
				entityManager.persist( book );
				entityManager.flush();
				//tag::schema-guide-assertions[]
				assertThat( book.edition ).isEqualTo( 1 );
				assertThat( book.titleLength ).isEqualTo( 9 );
				//end::schema-guide-assertions[]
				book.title = "Hibernate User Guide";
				entityManager.flush();
				//tag::schema-guide-assertions[]
				assertThat( book.titleLength ).isEqualTo( 20 );
				//end::schema-guide-assertions[]
				entityManager.getTransaction().commit();
			}
			//end::schema-guide-generated[]
			assertThatThrownBy( () -> insert( factory, new SchemaGuideBook( 2L, "Hibernate User Guide", "Hibernate" ) ) )
					.hasRootCauseInstanceOf( org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException.class );
			//tag::schema-guide-check-failure[]
			try (var entityManager = factory.createEntityManager()) {
				entityManager.getTransaction().begin();
				var book = entityManager.find( SchemaGuideBook.class, 1L );
				book.edition = 0;
				try {
					entityManager.flush();
					//tag::schema-guide-assertions[]
					throw new AssertionError( "The check constraint should reject edition 0" );
					//end::schema-guide-assertions[]
				}
				catch (jakarta.persistence.PersistenceException failure) {
					//tag::schema-guide-assertions[]
					assertThat( failure ).isInstanceOf( org.hibernate.exception.ConstraintViolationException.class );
					//end::schema-guide-assertions[]
					entityManager.getTransaction().rollback();
				}
			}
			//end::schema-guide-check-failure[]
			factory.getSchemaManager().drop( false );
		}
	}

	@Test
	void auxiliaryObjectsFollowSchemaActions() throws Exception {
		var configuration = configuration( "jdbc:h2:mem:schema_objects" );
		//tag::schema-guide-objects[]
		configuration.mappingFile( "org/hibernate/orm/test/schema/guide/objects.orm.xml" )
				.property( "jakarta.persistence.schema-generation.database.action", "none" );
		try (var factory = configuration.createEntityManagerFactory()) {
			//tag::schema-guide-assertions[]
			assertThat( exists( "jdbc:h2:mem:schema_objects", "SCHEMA_BOOK_TITLES" ) ).isFalse();
			//end::schema-guide-assertions[]
			factory.getSchemaManager().create( false );
			//tag::schema-guide-assertions[]
			assertThat( exists( "jdbc:h2:mem:schema_objects", "SCHEMA_BOOK_TITLES" ) ).isTrue();
			try (var entityManager = factory.createEntityManager()) {
				entityManager.createNativeQuery( "select title from schema_book_titles", String.class ).getResultList();
			}
			//end::schema-guide-assertions[]
			factory.getSchemaManager().drop( false );
			//tag::schema-guide-assertions[]
			assertThat( exists( "jdbc:h2:mem:schema_objects", "SCHEMA_BOOK_TITLES" ) ).isFalse();
			//end::schema-guide-assertions[]
		}
		//end::schema-guide-objects[]
		// The same mapping document contributes no H2-scoped view to PostgreSQL DDL.
		var createScript = new StringWriter();
		configuration.property( JdbcSettings.DIALECT, PostgreSQLDialect.class.getName() )
				.property( JdbcSettings.ALLOW_METADATA_ON_BOOT, false )
				.property( JdbcSettings.CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() )
				.property( JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create" )
				.property( JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createScript );
		Persistence.generateSchema( configuration );
		assertThat( createScript.toString().toLowerCase() ).contains( "create table schema_book" )
				.doesNotContain( "schema_book_titles" );
	}

	@Test
	void namedPersistenceUnitGeneration() throws Exception {
		var original = Thread.currentThread().getContextClassLoader();
		try (var loader = new URLClassLoader( new java.net.URL[] { getClass().getResource( "unit/" ) }, original )) {
			Thread.currentThread().setContextClassLoader( loader );
			//tag::schema-guide-named[]
			Map<String, Object> settings = Map.of(
					"jakarta.persistence.schema-generation.database.action", "drop-and-create",
					"jakarta.persistence.jdbc.url", "jdbc:h2:mem:schema_named;DB_CLOSE_DELAY=-1"
			);
			Persistence.generateSchema( "SchemaGuide", settings );
			//end::schema-guide-named[]
			try (var factory = configuration( "jdbc:h2:mem:schema_named" ).createEntityManagerFactory()) {
				factory.getSchemaManager().validate();
				factory.getSchemaManager().drop( false );
			}
		}
		finally {
			Thread.currentThread().setContextClassLoader( original );
		}
	}

	@Test
	void actionPrecedenceIsExplicit() {
		assertThat( ActionGrouping.interpret( Map.of( HBM2DDL_AUTO, "create", JAKARTA_HBM2DDL_DATABASE_ACTION, "none" ) ).databaseAction() )
				.isEqualTo( Action.NONE );
		var scripts = ActionGrouping.interpret( Map.of( HBM2DDL_AUTO, "create", JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create" ) );
		assertThat( scripts.databaseAction() ).isEqualTo( Action.NONE );
		assertThat( scripts.scriptAction() ).isEqualTo( Action.CREATE_ONLY );
		assertThat( ActionGrouping.interpret( Map.of( HBM2DDL_AUTO, "create" ) ).databaseAction() ).isEqualTo( Action.CREATE );
	}

	private static void insert(SessionFactory factory, SchemaGuideBook book) {
		try (var entityManager = factory.createEntityManager()) {
			entityManager.getTransaction().begin();
			try {
				entityManager.persist( book );
				entityManager.getTransaction().commit();
			}
			catch (RuntimeException failure) {
				if ( entityManager.getTransaction().isActive() ) {
					entityManager.getTransaction().rollback();
				}
				throw failure;
			}
		}
	}

	private static boolean exists(String url, String name) throws Exception {
		try (var connection = DriverManager.getConnection( url, "sa", "" );
				var result = connection.getMetaData().getTables( null, null, name, null )) {
			return result.next();
		}
	}
}
