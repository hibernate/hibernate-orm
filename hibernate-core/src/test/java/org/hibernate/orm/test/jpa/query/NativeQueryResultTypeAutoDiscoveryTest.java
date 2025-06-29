/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.hibernate.type.descriptor.jdbc.RealJdbcType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.CustomRunner;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.hamcrest.CoreMatchers.instanceOf;

/**
 * Test how the type of results are detected from the JDBC type in native queries,
 * when the type is not otherwise explicitly set on the query.
 *
 * This behavior is (more or less) implemented in implementations of
 * {@code org.hibernate.loader.custom.ResultColumnProcessor#performDiscovery(org.hibernate.loader.custom.JdbcResultMetadata, List, List)}.
 *
 * We use one entity type per JDBC type, just in case some types are not supported in some dialects,
 * so that we can more easily disable testing of a particular type in a particular dialect.
 */
@JiraKey(value = "HHH-7318")
@RunWith(CustomRunner.class)
public class NativeQueryResultTypeAutoDiscoveryTest {

	private static final Dialect DIALECT = DialectContext.getDialect();

	private SessionFactoryImplementor entityManagerFactory;

	@After
	public void cleanupEntityManagerFactory() {
		if ( entityManagerFactory != null ) {
			entityManagerFactory.close();
			entityManagerFactory = null;
		}
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle maps integer types to number")
	public void smallintType() {
		createEntityManagerFactory(SmallintEntity.class);
		doTest( SmallintEntity.class, (short)32767 );
	}

	@Test
	public void integerTypes() {
		createEntityManagerFactory(
				BigintEntity.class,
				IntegerEntity.class
		);

		doTest( BigintEntity.class, 9223372036854775807L );
		doTest( IntegerEntity.class, 2147483647 );
	}

	@Test
	public void doubleType() {
		createEntityManagerFactory( DoubleEntity.class );
		doTest( DoubleEntity.class, 445146115151.45845 );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "No support for the bit datatype so we use tinyint")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "No support for the bit datatype so we use number(1,0)")
	@SkipForDialect(dialectClass = DB2Dialect.class, majorVersion = 10, reason = "No support for the bit datatype so we use smallint")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "No support for the bit datatype so we use char(1)")
	public void booleanType() {
		createEntityManagerFactory( BooleanEntity.class );
		doTest( BooleanEntity.class, true );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "No support for the bit datatype so we use tinyint")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "No support for the bit datatype so we use number(1,0)")
	@SkipForDialect(dialectClass = DB2Dialect.class, majorVersion = 10, reason = "No support for the bit datatype so we use smallint")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "No support for the bit datatype so we use char(1)")
	public void bitType() {
		createEntityManagerFactory( BitEntity.class );
		doTest( BitEntity.class, false );
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, reason = "Turns tinyints into shorts in result sets and advertises the type as short in the metadata")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "Turns tinyints into shorts in result sets and advertises the type as short in the metadata")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Turns tinyints into shorts in result sets and advertises the type as short in the metadata")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(dialectClass = AbstractTransactSQLDialect.class, matchSubTypes = true, reason = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle maps tinyint to number")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase maps tinyint to smallint")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "informix maps tinyint to smallint")
	public void tinyintType() {
		createEntityManagerFactory( TinyintEntity.class );
		doTest( TinyintEntity.class, (byte)127 );
	}

	@Test
	@SkipForDialect(dialectClass = H2Dialect.class, reason = "Turns floats into doubles in result sets and advertises the type as double in the metadata")
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "Turns floats into doubles in result sets and advertises the type as double in the metadata")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Turns floats into doubles in result sets and advertises the type as double in the metadata")
	public void floatType() {
		createEntityManagerFactory( FloatEntity.class );
		doTest( FloatEntity.class, 15516.125f );
	}

	@Test
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "Turns reals into doubles in result sets and advertises the type as double in the metadata")
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "Turns reals into doubles in result sets and advertises the type as double in the metadata")
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "Turns reals into doubles in result sets and advertises the type as double in the metadata")
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "Turns reals into doubles in result sets and advertises the type as double in the metadata")
	public void realType() {
		createEntityManagerFactory( RealEntity.class );
		doTest( RealEntity.class, 15516.125f );
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Value is too big for the maximum allowed precision of Derby")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "Value is too big for the maximum allowed precision of DB2")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Value is too big for the maximum allowed precision of Oracle")
	@SkipForDialect(dialectClass = AbstractTransactSQLDialect.class, matchSubTypes = true, reason = "Value is too big for the maximum allowed precision of SQL Server and Sybase")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "Value is too big for the maximum allowed precision of HANA")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Value is too big for the maximum allowed precision of Firebird")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Value is too big for the maximum allowed precision of Altibase")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "The scale exceeds the maximum precision specified")
	public void numericType() {
		createEntityManagerFactory(
				NumericEntity.class
		);
		doTest( NumericEntity.class, new BigDecimal( "5464384284258458485484848458.48465843584584684" ) );
	}

	@Test
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Value is too big for the maximum allowed precision of Derby")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "Value is too big for the maximum allowed precision of DB2")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Value is too big for the maximum allowed precision of Oracle")
	@SkipForDialect(dialectClass = AbstractTransactSQLDialect.class, matchSubTypes = true, reason = "Value is too big for the maximum allowed precision of SQL Server and Sybase")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "Value is too big for the maximum allowed precision of HANA")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Value is too big for the maximum allowed precision of Firebird")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Value is too big for the maximum allowed precision of Altibase")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "The scale exceeds the maximum precision specified")
	public void decimalType() {
		createEntityManagerFactory( DecimalEntity.class );
		doTest( DecimalEntity.class, new BigDecimal( "5464384284258458485484848458.48465843584584684" )  );
	}

	@Test
	public void commonTextTypes() {
		createEntityManagerFactory(
				VarcharEntity.class,
				NvarcharEntity.class
		);

		doTest( VarcharEntity.class, "some text" );
		doTest( NvarcharEntity.class, "some text" );
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle maps LONGVARCHAR to CLOB")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 maps LONGVARCHAR to CLOB")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase maps LONGVARCHAR to CLOB")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA maps LONGVARCHAR to CLOB")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase maps LONGVARCHAR to CLOB")
	public void longCharType() {
		createEntityManagerFactory(
				LongvarcharEntity.class
		);

		doTest( LongvarcharEntity.class, "some text" );
	}

	@Test
	public void charType() {
		createEntityManagerFactory( CharEntity.class );
		doTest( CharEntity.class, 'c' );
	}

	@Test
	// Most other dialects define java.sql.Types.CHAR as "CHAR(1)" instead of "CHAR($l)", so they ignore the length
	@RequiresDialect(H2Dialect.class)
	public void char255Type() {
		createEntityManagerFactory( Char255Entity.class );
		doTest( Char255Entity.class, "some text" );
	}

	@Test
	public void binaryTypes() {
		createEntityManagerFactory(
				BinaryEntity.class,
				VarbinaryEntity.class
		);

		doTest( BinaryEntity.class, "some text".getBytes() );
		doTest( VarbinaryEntity.class, "some text".getBytes() );
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle maps LONGVARBINARY to BLOB")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 maps LONGVARBINARY to BLOB")
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase maps LONGVARBINARY to BLOB")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase maps LONGVARBINARY to BLOB")
	@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA maps LONGVARCHAR to BLOB")
	public void longBinaryType() {
		createEntityManagerFactory(
				LongvarbinaryEntity.class
		);

		doTest( LongvarbinaryEntity.class, "some text".getBytes() );
	}

	@Test
	// Lobs are apparently handled differently in other dialects, queries return Strings instead of the CLOB/BLOB
	@RequiresDialect(H2Dialect.class)
	public void lobTypes() {
		createEntityManagerFactory(
				ClobEntity.class,
				BlobEntity.class
		);

		doTest(
				ClobEntity.class,
				Clob.class,
				session -> session.getLobHelper().createClob( "some text" )
		);
		doTest(
				BlobEntity.class,
				Blob.class,
				session -> session.getLobHelper().createBlob( "some text".getBytes() )
		);
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle maps DATE and TIME to TIMESTAMP")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "EDB maps DATE and TIME to TIMESTAMP")
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Sybase maps DATE and TIME to TIMESTAMP", matchSubTypes = true)
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase maps DATE and TIME to TIMESTAMP")
	public void dateTimeTypes() {
		createEntityManagerFactory(
				DateEntity.class,
				TimeEntity.class
		);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(
				2014, Month.NOVEMBER.getValue(), 15,
				18, 0, 0, 0,
				ZoneId.of( "UTC" )
		);

		doTest( DateEntity.class, new java.sql.Date( zonedDateTime.toInstant().toEpochMilli() ) );
		doTest( TimeEntity.class, new Time( zonedDateTime.toLocalTime().toNanoOfDay() / 1000 ) );
	}

	@Test
	public void timestampType() {
		createEntityManagerFactory(
				TimestampEntity.class
		);

		ZonedDateTime zonedDateTime = ZonedDateTime.of(
				2014, Month.NOVEMBER.getValue(), 15,
				18, 0, 0, 0,
				ZoneId.of( "UTC" )
		);

		doTest( TimestampEntity.class, new Timestamp( zonedDateTime.toInstant().toEpochMilli() ) );
	}

	private <E extends TestedEntity<T>, T> void doTest(Class<E> entityType, T testedValue) {
		this.doTest( entityType, ReflectHelper.getClass( testedValue ), ignored -> testedValue );
	}

	private <E extends TestedEntity<T>, T> void doTest(Class<E> entityType, Class<? extends T> testedValueClass,
			Function<Session, T> testedValueProvider) {
		String entityName = entityManagerFactory.getMetamodel().entity( entityType ).getName();
		// Expecting all entities to use the entity name as table name in these tests, because it's simpler
		String tableName = entityName;

		// Create a single record in the test database.
		TransactionUtil.doInJPA( () -> entityManagerFactory, em -> {
			try {
				E entity = entityType.getConstructor().newInstance();
				T testedValue = testedValueProvider.apply( em.unwrap( Session.class ) );
				entity.setTestedProperty( testedValue );
				em.persist( entity );
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new IllegalStateException( "Unexpected checked exception: " + e.getMessage(), e );
			}
		} );

		TransactionUtil.doInJPA( () -> entityManagerFactory, em -> {
			// Execute a native query to get the entity that was just created.
			Object result = em.createNativeQuery(
					"SELECT testedProperty FROM " + tableName
			)
					.getSingleResult();

			Assert.assertThat( result, instanceOf( testedValueClass ) );
		} );
	}

	private void createEntityManagerFactory(Class<?> ... entityTypes) {
		cleanupEntityManagerFactory();
		EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder =
				(EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(
						new PersistenceUnitDescriptorAdapter(),
						buildSettings( entityTypes )
				);
		entityManagerFactory = entityManagerFactoryBuilder.build().unwrap( SessionFactoryImplementor.class );
	}

	private Map<Object, Object> buildSettings(Class<?> ... entityTypes) {
		Map<Object, Object> settings = new HashMap<>();
		settings.put( AvailableSettings.NATIVE_PREFER_JDBC_DATETIME_TYPES, "true" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		settings.put( AvailableSettings.DIALECT, DIALECT.getClass().getName() );
		settings.put( AvailableSettings.LOADED_CLASSES, Arrays.asList( entityTypes ) );
		ServiceRegistryUtil.applySettings( settings );
		return settings;
	}

	@MappedSuperclass
	static abstract class TestedEntity<T> {
		private Long id;

		protected T testedProperty;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		protected void setId(Long id) {
			this.id = id;
		}

		// Only define the setter here, not the getter, so that subclasses can define the @Type/@Column themselves
		public void setTestedProperty(T value) {
			this.testedProperty = value;
		}
	}

	@Entity(name = "bigintEntity")
	public static class BigintEntity extends TestedEntity<Long> {
		public Long getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "integerEntity")
	public static class IntegerEntity extends TestedEntity<Integer> {
		public Integer getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "smallintEntity")
	public static class SmallintEntity extends TestedEntity<Short> {
		public Short getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "tinyintEntity")
	public static class TinyintEntity extends TestedEntity<Byte> {
		public Byte getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "doubleEntity")
	public static class DoubleEntity extends TestedEntity<Double> {
		public Double getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "floatEntity")
	public static class FloatEntity extends TestedEntity<Float> {
		public Float getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "realEntity")
	public static class RealEntity extends TestedEntity<Float> {
		/**
		 * The custom type sets the SQL type to {@link Types#REAL}
		 * instead of the default {@link Types#FLOAT}.
		 */
		@JdbcTypeCode( Types.REAL )
		public Float getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "numericEntity")
	public static class NumericEntity extends TestedEntity<BigDecimal> {
		@Column(precision = 50, scale = 15)
		public BigDecimal getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "decimalEntity")
	public static class DecimalEntity extends TestedEntity<BigDecimal> {
		/**
		 * The custom type sets the SQL type to {@link Types#DECIMAL}
		 * instead of the default {@link Types#NUMERIC}.
		 */
		@JdbcTypeCode( Types.DECIMAL )
		@Column(precision = 50, scale = 15)
		public BigDecimal getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "varcharEntity")
	public static class VarcharEntity extends TestedEntity<String> {
		public String getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "nvarcharEntity")
	public static class NvarcharEntity extends TestedEntity<String> {
		@Nationalized
		public String getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "charEntity")
	public static class CharEntity extends TestedEntity<Character> {
		@Column(length = 1)
		public Character getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "char255Entity")
	public static class Char255Entity extends TestedEntity<String> {
		/**
		 * The custom type sets the SQL type to {@link Types#CHAR}
		 * instead of the default {@link Types#VARCHAR}.
		 */
		@JdbcTypeCode( Types.CHAR )
		@Column(length = 255)
		public String getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "longvarcharEntity")
	public static class LongvarcharEntity extends TestedEntity<String> {
		/**
		 * Use {@link Types#LONGVARCHAR} instead of the default {@link Types#VARCHAR}.
		 */
		@JdbcTypeCode( Types.LONGVARCHAR )
		public String getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "binaryEntity")
	public static class BinaryEntity extends TestedEntity<byte[]> {
		/**
		 * The custom type sets the SQL type to {@link Types#BINARY}
		 * instead of the default {@link Types#VARBINARY}.
		 */
		@JdbcTypeCode( Types.BINARY )
		public byte[] getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "varbinaryEntity")
	public static class VarbinaryEntity extends TestedEntity<byte[]> {
		public byte[] getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "longvarbinaryEntity")
	public static class LongvarbinaryEntity extends TestedEntity<byte[]> {
		/**
		 * Use {@link Types#LONGVARBINARY} instead of the default {@link Types#VARBINARY}.
		 */
		@JdbcTypeCode( Types.LONGVARBINARY )
		public byte[] getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "clobEntity")
	public static class ClobEntity extends TestedEntity<Clob> {
		public Clob getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "blobEntity")
	public static class BlobEntity extends TestedEntity<Blob> {
		public Blob getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "dateEntity")
	public static class DateEntity extends TestedEntity<java.sql.Date> {
		public java.sql.Date getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "timeEntity")
	public static class TimeEntity extends TestedEntity<Time> {
		public Time getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "timestampEntity")
	public static class TimestampEntity extends TestedEntity<Timestamp> {
		public Timestamp getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "booleanEntity")
	public static class BooleanEntity extends TestedEntity<Boolean> {
		public Boolean getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "bitEntity")
	public static class BitEntity extends TestedEntity<Boolean> {
		@JdbcTypeCode(Types.BIT)
		public Boolean getTestedProperty() {
			return testedProperty;
		}
	}

	public static class FloatAsRealType extends AbstractSingleColumnStandardBasicType<Float> {
		public static final String NAME = "float_as_real";

		public FloatAsRealType() {
			super( RealJdbcType.INSTANCE, FloatJavaType.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class BigDecimalAsDecimalType extends AbstractSingleColumnStandardBasicType<BigDecimal> {
		public static final String NAME = "big_decimal_as_decimal";

		public BigDecimalAsDecimalType() {
			super( NumericJdbcType.INSTANCE, BigDecimalJavaType.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class StringAsNonVarCharType extends AbstractSingleColumnStandardBasicType<String> {
		public static final String NAME = "string_as_nonvar_char_array";

		public StringAsNonVarCharType() {
			super( CharJdbcType.INSTANCE, StringJavaType.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class ByteArrayAsNonVarBinaryType extends AbstractSingleColumnStandardBasicType<byte[]> {
		public static final String NAME = "byte_array_as_nonvar_binary";

		public ByteArrayAsNonVarBinaryType() {
			super( BinaryJdbcType.INSTANCE, PrimitiveByteArrayJavaType.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

}
