/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BlobType;
import org.hibernate.type.CharArrayType;
import org.hibernate.type.ClobType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.Type;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * A test asserting LONG/LONGRAW versus CLOB/BLOB resolution for various Oracle Dialects
 *
 * @author Steve Ebersole
 */
public class OracleLongLobTypeTest extends BaseUnitTestCase {
	@Test
	public void testOracle8() {
		check( Oracle8iDialect.class, Primitives.class, BinaryType.class, CharArrayType.class );
		check( Oracle8iDialect.class, LobPrimitives.class, MaterializedBlobType.class, PrimitiveCharacterArrayClobType.class );
		check( Oracle8iDialect.class, LobLocators.class, BlobType.class, ClobType.class );
	}

	@Test
	public void testOracle9() {
		check( Oracle9iDialect.class, Primitives.class, BinaryType.class, CharArrayType.class );
		check( Oracle9iDialect.class, LobPrimitives.class, MaterializedBlobType.class, PrimitiveCharacterArrayClobType.class );
		check( Oracle9iDialect.class, LobLocators.class, BlobType.class, ClobType.class );
	}

	@Test
	public void testOracle10() {
		check( Oracle10gDialect.class, Primitives.class, BinaryType.class, CharArrayType.class );
		check( Oracle10gDialect.class, LobPrimitives.class, MaterializedBlobType.class, PrimitiveCharacterArrayClobType.class );
		check( Oracle10gDialect.class, LobLocators.class, BlobType.class, ClobType.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10345" )
	public void testOracle12() {
		check( Oracle12cDialect.class, Primitives.class, MaterializedBlobType.class, CharArrayType.class );
		check( Oracle12cDialect.class, LobPrimitives.class, MaterializedBlobType.class, PrimitiveCharacterArrayClobType.class );
		check( Oracle12cDialect.class, LobLocators.class, BlobType.class, ClobType.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10345" )
	public void testOracle12PreferLongRaw() {
		check( Oracle12cDialect.class, Primitives.class, BinaryType.class, CharArrayType.class, true );
		check( Oracle12cDialect.class, LobPrimitives.class, MaterializedBlobType.class, PrimitiveCharacterArrayClobType.class, true );
		check( Oracle12cDialect.class, LobLocators.class, BlobType.class, ClobType.class, true );
	}

	private void check(
			Class<? extends Dialect> dialectClass,
			Class entityClass,
			Class<? extends Type> binaryTypeClass,
			Class<? extends Type> charTypeClass) {
		check( dialectClass, entityClass, binaryTypeClass, charTypeClass, false );
	}

	private void check(
			Class<? extends Dialect> dialectClass,
			Class entityClass,
			Class<? extends Type> binaryTypeClass,
			Class<? extends Type> charTypeClass,
			boolean preferLongRaw) {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, dialectClass.getName() )
				.applySetting( Oracle12cDialect.PREFER_LONG_RAW, Boolean.toString( preferLongRaw ) )
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", false )
				.build();

		try {
			final MetadataImplementor mappings = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( entityClass )
					.buildMetadata();
			mappings.validate();

			final PersistentClass entityBinding = mappings.getEntityBinding( entityClass.getName() );

			assertThat( entityBinding.getProperty( "binaryData" ).getType(), instanceOf( binaryTypeClass ) );
			assertThat( entityBinding.getProperty( "characterData" ).getType(), instanceOf( charTypeClass ) );
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
