package org.hibernate.orm.test.schemaupdate;

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.type.SqlTypes;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.util.EnumSet;

/**
 * @author Nils Israel, adapted test case from Yoann Rodiere
 */
@JiraKey("HHH-16360")
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(H2Dialect.class)
class SchemaUpdateAfterDDLCreateTest extends BaseNonConfigCoreFunctionalTestCase {
	protected static ServiceRegistry serviceRegistry;
	protected static MetadataImplementor metadata;
	private static final String DELIMITER = ";";

	@BeforeAll
	public static void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "false" )
				.applySetting( Environment.DEFAULT_SCHEMA, "public" )
				.build();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClasses( getEntities() )
				.buildMetadata();
		new SchemaUpdate()
			.setHaltOnError( true )
			.setDelimiter( DELIMITER )
			.setFormat( true )
			.execute( EnumSet.of( TargetType.DATABASE ), metadata);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3})
	void testEntities(int index) throws Exception {
		setUp();
			File output = File.createTempFile( "update_script", ".sql" );
			output.deleteOnExit();

			Metadata updateMetadata = new MetadataSources(serviceRegistry)
				.addAnnotatedClass(getEntities()[index])
				.buildMetadata();

			new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setDelimiter( DELIMITER )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ), updateMetadata);

			String outputContent = new String(Files.readAllBytes(output.toPath()));

			Assert.assertEquals("", outputContent);
	}


	@AfterAll
	public static void tearDown() {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Entity
	public static class EntityWithVarchar {
		@Id
		@GeneratedValue
		Long id;
		String topic;
	}

	@Entity
	public static class EntityWithText {
		@Id
		@GeneratedValue
		Long id;
		@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
		String contents;

	}

	@Entity
	public static class EntityWithDate {
		@Id
		@GeneratedValue
		Long id;
		Date aDate = new Date();
	}

	@Entity
	public static class EntityWithDouble {
		@Id
		@GeneratedValue
		Long id;
		Double aDouble = 0.0;
	}

	protected static Class[] getEntities() {
		return new Class[]{
			EntityWithVarchar.class,
			EntityWithDate.class,
			EntityWithText.class,
			EntityWithDouble.class
		};
	}
}
