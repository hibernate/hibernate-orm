/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.BigDecimalJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.FloatTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.CharJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NumericJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.RealJdbcTypeDescriptor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.CustomRunner;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
@TestForIssue(jiraKey = "HHH-7318")
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
	public void commonNumericTypes() {
		createEntityManagerFactory(
				BigintEntity.class,
				IntegerEntity.class,
				SmallintEntity.class,
				DoubleEntity.class
		);

		doTest( BigintEntity.class, 9223372036854775807L );
		doTest( IntegerEntity.class, 2147483647 );
		doTest( SmallintEntity.class, (short)32767 );
		doTest( DoubleEntity.class, 445146115151.45845 );
	}

	@Test
	@SkipForDialect(value = SybaseDialect.class, comment = "No support for the bit datatype so we use tinyint")
	public void booleanType() {
		createEntityManagerFactory( BooleanEntity.class );
		doTest( BooleanEntity.class, true );
	}

	@Test
	@SkipForDialect(value = SybaseDialect.class, comment = "No support for the bit datatype so we use tinyint")
	public void bitType() {
		createEntityManagerFactory( BitEntity.class );
		doTest( BitEntity.class, false );
	}

	@Test
	@SkipForDialect(value = PostgreSQLDialect.class, comment = "Turns tinyints into shorts in result sets and advertises the type as short in the metadata")
	@SkipForDialect(value = DerbyDialect.class, comment = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(value = DB2Dialect.class, comment = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(value = AbstractTransactSQLDialect.class, comment = "No support for the tinyint datatype so we use smallint")
	@SkipForDialect(value = AbstractHANADialect.class, comment = "No support for the tinyint datatype so we use smallint")
	public void tinyintType() {
		createEntityManagerFactory( TinyintEntity.class );
		doTest( TinyintEntity.class, (byte)127 );
	}

	@Test
	@SkipForDialect(value = H2Dialect.class, comment = "Turns floats into doubles in result sets and advertises the type as double in the metadata")
	@SkipForDialect(value = HSQLDialect.class, comment = "Turns floats into doubles in result sets and advertises the type as double in the metadata")
	public void floatType() {
		createEntityManagerFactory( FloatEntity.class );
		doTest( FloatEntity.class, 15516.125f );
	}

	@Test
	@SkipForDialect(value = MySQLDialect.class, comment = "Turns reals into doubles in result sets and advertises the type as double in the metadata")
	@SkipForDialect(value = HSQLDialect.class, comment = "Turns reals into doubles in result sets and advertises the type as double in the metadata")
	public void realType() {
		createEntityManagerFactory( RealEntity.class );
		doTest( RealEntity.class, 15516.125f );
	}

	@Test
	@SkipForDialect(value = DerbyDialect.class, comment = "Value is too big for the maximum allowed precision of Derby")
	@SkipForDialect(value = DB2Dialect.class, comment = "Value is too big for the maximum allowed precision of DB2")
	@SkipForDialect(value = OracleDialect.class, comment = "Value is too big for the maximum allowed precision of Oracle")
	@SkipForDialect(value = AbstractTransactSQLDialect.class, comment = "Value is too big for the maximum allowed precision of SQL Server and Sybase")
	@SkipForDialect(value = AbstractHANADialect.class, comment = "Value is too big for the maximum allowed precision of HANA")
	public void numericType() {
		createEntityManagerFactory(
				NumericEntity.class
		);
		doTest( NumericEntity.class, new BigDecimal( "5464384284258458485484848458.48465843584584684" ) );
	}

	@Test
	@SkipForDialect(value = DerbyDialect.class, comment = "Value is too big for the maximum allowed precision of Derby")
	@SkipForDialect(value = DB2Dialect.class, comment = "Value is too big for the maximum allowed precision of DB2")
	@SkipForDialect(value = OracleDialect.class, comment = "Value is too big for the maximum allowed precision of Oracle")
	@SkipForDialect(value = AbstractTransactSQLDialect.class, comment = "Value is too big for the maximum allowed precision of SQL Server and Sybase")
	@SkipForDialect(value = AbstractHANADialect.class, comment = "Value is too big for the maximum allowed precision of HANA")
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
	@SkipForDialect(value = OracleDialect.class, comment = "Oracle maps LONGVARCHAR to CLOB")
	@SkipForDialect(value = DB2Dialect.class, comment = "DB2 maps LONGVARCHAR to CLOB")
	@SkipForDialect(value = SybaseDialect.class, comment = "Sybase maps LONGVARCHAR to CLOB")
	@SkipForDialect(value = AbstractHANADialect.class, comment = "HANA maps LONGVARCHAR to CLOB")
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
	@SkipForDialect(value = OracleDialect.class, comment = "Oracle maps LONGVARBINARY to BLOB")
	@SkipForDialect(value = DB2Dialect.class, comment = "DB2 maps LONGVARBINARY to BLOB")
	@SkipForDialect(value = SybaseDialect.class, comment = "Sybase maps LONGVARBINARY to BLOB")
	@SkipForDialect(value = AbstractHANADialect.class, comment = "HANA maps LONGVARCHAR to BLOB")
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
				session -> Hibernate.getLobCreator( session ).createClob( "some text" )
		);
		doTest(
				BlobEntity.class,
				Blob.class,
				session -> Hibernate.getLobCreator( session ).createBlob( "some text".getBytes() )
		);
	}

	@Test
	@SkipForDialect(value = OracleDialect.class, comment = "Oracle maps DATE and TIME to TIMESTAMP")
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

	@SuppressWarnings("unchecked")
	private <E extends TestedEntity<T>, T> void doTest(Class<E> entityType, T testedValue) {
		this.doTest( entityType, (Class<? extends T>) testedValue.getClass(), ignored -> testedValue );
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

		settings.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		settings.put( org.hibernate.cfg.AvailableSettings.DIALECT, DIALECT.getClass().getName() );
		settings.put( AvailableSettings.LOADED_CLASSES, Arrays.asList( entityTypes ) );

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
	@TypeDef(name = FloatAsRealType.NAME, typeClass = FloatAsRealType.class)
	public static class RealEntity extends TestedEntity<Float> {
		/**
		 * The custom type sets the SQL type to {@link Types#REAL}
		 * instead of the default {@link Types#FLOAT}.
		 */
		@Type(type = FloatAsRealType.NAME)
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
	@TypeDef(name = BigDecimalAsDecimalType.NAME, typeClass = BigDecimalAsDecimalType.class)
	public static class DecimalEntity extends TestedEntity<BigDecimal> {
		/**
		 * The custom type sets the SQL type to {@link Types#DECIMAL}
		 * instead of the default {@link Types#NUMERIC}.
		 */
		@Type(type = BigDecimalAsDecimalType.NAME)
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
	@TypeDef(name = StringAsNonVarCharType.NAME, typeClass = StringAsNonVarCharType.class)
	public static class Char255Entity extends TestedEntity<String> {
		/**
		 * The custom type sets the SQL type to {@link Types#CHAR}
		 * instead of the default {@link Types#VARCHAR}.
		 */
		@Type(type = StringAsNonVarCharType.NAME)
		@Column(length = 255)
		public String getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "longvarcharEntity")
	public static class LongvarcharEntity extends TestedEntity<String> {
		/**
		 * The custom type sets the SQL type to {@link Types#LONGVARCHAR}
		 * instead of the default {@link Types#VARCHAR}.
		 */
		@Type(type = "text")
		public String getTestedProperty() {
			return testedProperty;
		}
	}

	@Entity(name = "binaryEntity")
	@TypeDef(name = ByteArrayAsNonVarBinaryType.NAME, typeClass = ByteArrayAsNonVarBinaryType.class)
	public static class BinaryEntity extends TestedEntity<byte[]> {
		/**
		 * The custom type sets the SQL type to {@link Types#BINARY}
		 * instead of the default {@link Types#VARBINARY}.
		 */
		@Type(type = ByteArrayAsNonVarBinaryType.NAME)
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
		 * The custom type sets the SQL type to {@link Types#LONGVARBINARY}
		 * instead of the default {@link Types#VARBINARY}.
		 */
		@Type(type = "image")
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
			super( RealJdbcTypeDescriptor.INSTANCE, FloatTypeDescriptor.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class BigDecimalAsDecimalType extends AbstractSingleColumnStandardBasicType<BigDecimal> {
		public static final String NAME = "big_decimal_as_decimal";

		public BigDecimalAsDecimalType() {
			super( NumericJdbcTypeDescriptor.INSTANCE, BigDecimalJavaTypeDescriptor.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class StringAsNonVarCharType extends AbstractSingleColumnStandardBasicType<String> {
		public static final String NAME = "string_as_nonvar_char_array";

		public StringAsNonVarCharType() {
			super( CharJdbcTypeDescriptor.INSTANCE, StringJavaTypeDescriptor.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class ByteArrayAsNonVarBinaryType extends AbstractSingleColumnStandardBasicType<byte[]> {
		public static final String NAME = "byte_array_as_nonvar_binary";

		public ByteArrayAsNonVarBinaryType() {
			super( BinaryJdbcTypeDescriptor.INSTANCE, PrimitiveByteArrayJavaTypeDescriptor.INSTANCE );
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

}

