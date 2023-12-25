/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.sql.Blob;
import java.sql.Clob;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * A test asserting LONG/LONGRAW versus CLOB/BLOB resolution for various Oracle Dialects
 *
 * @author Steve Ebersole
 */
public class OracleLongLobTypeTest extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-10345" )
	public void testOracle12() {
		check( OracleDialect.class, Primitives.class, StandardBasicTypes.BINARY, StandardBasicTypes.CHAR_ARRAY );
		check( OracleDialect.class, LobPrimitives.class, StandardBasicTypes.MATERIALIZED_BLOB, StandardBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY );
		check( OracleDialect.class, LobLocators.class, StandardBasicTypes.BLOB, StandardBasicTypes.CLOB );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10345" )
	public void testOracle12PreferLongRaw() {
		check( OracleDialect.class, Primitives.class, StandardBasicTypes.BINARY, StandardBasicTypes.CHAR_ARRAY, true );
		check( OracleDialect.class, LobPrimitives.class, StandardBasicTypes.MATERIALIZED_BLOB, StandardBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY, true );
		check( OracleDialect.class, LobLocators.class, StandardBasicTypes.BLOB, StandardBasicTypes.CLOB, true );
	}

	private void check(
			Class<? extends Dialect> dialectClass,
			Class entityClass,
			BasicTypeReference<?> binaryTypeClass,
			BasicTypeReference<?> charTypeClass) {
		check( dialectClass, entityClass, binaryTypeClass, charTypeClass, false );
	}

	private void check(
			Class<? extends Dialect> dialectClass,
			Class entityClass,
			BasicTypeReference<?> binaryTypeClass,
			BasicTypeReference<?> charTypeClass,
			boolean preferLongRaw) {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, dialectClass.getName() )
				.applySetting( OracleDialect.PREFER_LONG_RAW, Boolean.toString( preferLongRaw ) )
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", false )
				.build();

		try {
			final MetadataImplementor mappings = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( entityClass )
					.buildMetadata();
			mappings.orderColumns( false );
			mappings.validate();

			final PersistentClass entityBinding = mappings.getEntityBinding( entityClass.getName() );
			final JdbcTypeRegistry jdbcTypeRegistry = mappings.getTypeConfiguration()
					.getJdbcTypeRegistry();

			BasicType<?> type;

			type = (BasicType<?>) entityBinding.getProperty( "binaryData" ).getType();
			assertSame( jdbcTypeRegistry.getDescriptor( binaryTypeClass.getSqlTypeCode() ), type.getJdbcType() );
			type = (BasicType<?>) entityBinding.getProperty( "characterData" ).getType();
			assertSame( jdbcTypeRegistry.getDescriptor( charTypeClass.getSqlTypeCode() ), type.getJdbcType() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity
	public static class Primitives {
		@Id
		public Integer id;
		public byte[] binaryData;
		public char[] characterData;
	}

	@Entity
	public static class LobPrimitives {
		@Id
		public Integer id;
		@Lob
		public byte[] binaryData;
		@Lob
		public char[] characterData;
	}

	@Entity
	public static class LobLocators {
		@Id
		public Integer id;
		@Lob
		public Blob binaryData;
		@Lob
		public Clob characterData;
	}
}
