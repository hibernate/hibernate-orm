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
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Date;
import java.util.EnumSet;

/**
 * @author Nils Israel, adapted a test case from Yoann Rodiere
 *
 * Create the schema based on the provided entities and check for each entity individually whether the
 * in 6.2 added possiblilty to change column types in schema update wants to change the schema afterward.
 * That shouldn't be the case.
 */
@JiraKey("HHH-16360")
class SchemaUpdateAfterDDLCreateTest extends BaseNonConfigCoreFunctionalTestCase {
	protected static ServiceRegistry serviceRegistry;
	protected static MetadataImplementor metadata;
	private static final String DELIMITER = ";";

	@BeforeAll
	public static void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
			.applySetting(Environment.GLOBALLY_QUOTED_IDENTIFIERS, "false")
			.applySetting(Environment.DEFAULT_SCHEMA, "public")
			.build();
		metadata = (MetadataImplementor) new MetadataSources(serviceRegistry)
			.addAnnotatedClasses(getEntities())
			.buildMetadata();
		new SchemaUpdate()
			.setHaltOnError(true)
			.setDelimiter(DELIMITER)
			.setFormat(true)
			.execute(EnumSet.of(TargetType.DATABASE), metadata);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
	void testEntities(int index) throws Exception {
		setUp();
		File output = File.createTempFile("update_script", ".sql");
		output.deleteOnExit();

		Metadata updateMetadata = new MetadataSources(serviceRegistry)
			.addAnnotatedClass(getEntities()[index])
			.buildMetadata();

		new SchemaUpdate()
			.setHaltOnError(true)
			.setOutputFile(output.getAbsolutePath())
			.setDelimiter(DELIMITER)
			.setFormat(true)
			.execute(EnumSet.of(TargetType.SCRIPT), updateMetadata);

		String outputContent = new String(Files.readAllBytes(output.toPath()));

		Assert.assertEquals("", outputContent);
	}


	@AfterAll
	public static void tearDown() {
		new SchemaExport().drop(EnumSet.of(TargetType.DATABASE, TargetType.STDOUT), metadata);
		StandardServiceRegistryBuilder.destroy(serviceRegistry);
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

	@Entity
	public static class EntityWithFloat {
		@Id
		@GeneratedValue
		Long id;
		float aFloat = 0.0f;
	}

	@Entity
	public static class EntityWithInt {
		@Id
		@GeneratedValue
		Long id;
		Integer aInt = 0;
	}

	@Entity
	public static class EntityWithBigDecimal {
		@Id
		@GeneratedValue
		Long id;
		BigDecimal aBigDecimal = BigDecimal.ZERO;
	}

	@Entity
	public static class EntityWithBigInteger {
		@Id
		@GeneratedValue
		Long id;
		BigInteger aBigInteger = BigInteger.ZERO;
	}

	@Entity
	public static class EntityWithBoolean {
		@Id
		@GeneratedValue
		Long id;
		Boolean aBoolean = false;
	}

	protected static Class[] getEntities() {
		return new Class[]{
			EntityWithVarchar.class,
			EntityWithDate.class,
			EntityWithText.class,
			EntityWithDouble.class,
			EntityWithFloat.class,
			EntityWithInt.class,
			EntityWithBigDecimal.class,
			EntityWithBigInteger.class,
			EntityWithBoolean.class
		};
	}
}
