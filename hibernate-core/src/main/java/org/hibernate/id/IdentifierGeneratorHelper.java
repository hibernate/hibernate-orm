/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.generator.values.internal.GeneratedValuesHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Factory and helper methods for {@link IdentifierGenerator} framework.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@Internal
public final class IdentifierGeneratorHelper {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( IdentifierGeneratorHelper.class );

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that we should
	 * short-circuit any continued generated id checking. Currently, this is only used in the case of the
	 * {@linkplain ForeignGenerator foreign} generator as a way to signal that we should use the associated
	 * entity's id value.
	 *
	 * @deprecated This is not an elegant way to do anything
	 */
	@Deprecated(forRemoval = true, since = "6.2")
	public static final Serializable SHORT_CIRCUIT_INDICATOR = new Serializable() {
		@Override
		public String toString() {
			return "SHORT_CIRCUIT_INDICATOR";
		}
	};

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that the entity's
	 * identifier will be generated as part of the database insertion.
	 *
	 * @deprecated Use a {@link org.hibernate.generator.OnExecutionGenerator}
	 */
	@Deprecated(forRemoval = true, since = "6.2")
	public static final Serializable POST_INSERT_INDICATOR = new Serializable() {
		@Override
		public String toString() {
			return "POST_INSERT_INDICATOR";
		}
	};

	/**
	 * Get the generated identifier when using identity columns
	 *
	 * @param path           The {@link NavigableRole#getFullPath()}
	 * @param resultSet      The result set from which to extract the generated identity
	 * @param wrapperOptions The session
	 * @return The generated identity value
	 * @throws SQLException       Can be thrown while accessing the result set
	 * @throws HibernateException Indicates a problem reading back a generated identity value.
	 *
	 * @deprecated Use {@link GeneratedValuesHelper#getGeneratedValues} instead
	 */
	@Deprecated( since = "6.5", forRemoval = true )
	public static Object getGeneratedIdentity(
			String path,
			ResultSet resultSet,
			PostInsertIdentityPersister persister,
			WrapperOptions wrapperOptions) throws SQLException {
		if ( !resultSet.next() ) {
			throw new HibernateException( "The database returned no natively generated identity value : " + path );
		}

		JdbcMapping identifierType = ( (SqlTypedMapping) persister.getIdentifierMapping() ).getJdbcMapping();
		Object id = identifierType.getJdbcValueExtractor()
				.extract( resultSet, columnIndex( resultSet, persister ), wrapperOptions );
		LOG.debugf( "Natively generated identity (%s) : %s", path, id );
		return id;
	}

	private static int columnIndex(ResultSet resultSet, PostInsertIdentityPersister persister) {
		try {
			ResultSetMetaData metaData = resultSet.getMetaData();
			String keyColumnName = persister.getRootTableKeyColumnNames()[0];
			Dialect dialect = persister.getFactory().getJdbcServices().getDialect();
			for ( int i = 1 ; i<=metaData.getColumnCount(); i++ ) {
				if ( equal( keyColumnName, metaData.getColumnName(i), dialect ) ) {
					return i;
				}
			}
		}
		catch (SQLException e) {
			LOG.debugf( "Could not determine column index from JDBC metadata", e );
		}
		return 1;
	}

	private static boolean equal(String keyColumnName, String alias, Dialect dialect) {
		return alias.equalsIgnoreCase( keyColumnName )
				|| alias.equalsIgnoreCase( StringHelper.unquote( keyColumnName, dialect ) );
	}

	public static IntegralDataTypeHolder getIntegralDataTypeHolder(Class<?> integralType) {
		if ( integralType == Long.class
				|| integralType == Integer.class
				|| integralType == Short.class ) {
			return new BasicHolder( integralType );
		}
		else if ( integralType == BigInteger.class ) {
			return new BigIntegerHolder();
		}
		else if ( integralType == BigDecimal.class ) {
			return new BigDecimalHolder();
		}
		else {
			throw new IdentifierGenerationException(
					"Unknown integral data type for ids : " + integralType.getName()
			);
		}
	}

	public static long extractLong(IntegralDataTypeHolder holder) {
		if ( holder.getClass() == BasicHolder.class ) {
			( (BasicHolder) holder ).checkInitialized();
			return ( (BasicHolder) holder ).value;
		}
		else if ( holder.getClass() == BigIntegerHolder.class ) {
			( (BigIntegerHolder) holder ).checkInitialized();
			return ( (BigIntegerHolder) holder ).value.longValue();
		}
		else if ( holder.getClass() == BigDecimalHolder.class ) {
			( (BigDecimalHolder) holder ).checkInitialized();
			return ( (BigDecimalHolder) holder ).value.longValue();
		}
		throw new IdentifierGenerationException( "Unknown IntegralDataTypeHolder impl [" + holder + "]" );
	}

	public static BigInteger extractBigInteger(IntegralDataTypeHolder holder) {
		if ( holder.getClass() == BasicHolder.class ) {
			( (BasicHolder) holder ).checkInitialized();
			return BigInteger.valueOf( ( (BasicHolder) holder ).value );
		}
		else if ( holder.getClass() == BigIntegerHolder.class ) {
			( (BigIntegerHolder) holder ).checkInitialized();
			return ( (BigIntegerHolder) holder ).value;
		}
		else if ( holder.getClass() == BigDecimalHolder.class ) {
			( (BigDecimalHolder) holder ).checkInitialized();
			// scale should already be set...
			return ( (BigDecimalHolder) holder ).value.toBigInteger();
		}
		throw new IdentifierGenerationException( "Unknown IntegralDataTypeHolder impl [" + holder + "]" );
	}

	public static BigDecimal extractBigDecimal(IntegralDataTypeHolder holder) {
		if ( holder.getClass() == BasicHolder.class ) {
			( (BasicHolder) holder ).checkInitialized();
			return BigDecimal.valueOf( ( (BasicHolder) holder ).value );
		}
		else if ( holder.getClass() == BigIntegerHolder.class ) {
			( (BigIntegerHolder) holder ).checkInitialized();
			return new BigDecimal( ( (BigIntegerHolder) holder ).value );
		}
		else if ( holder.getClass() == BigDecimalHolder.class ) {
			( (BigDecimalHolder) holder ).checkInitialized();
			// scale should already be set...
			return ( (BigDecimalHolder) holder ).value;
		}
		throw new IdentifierGenerationException( "Unknown IntegralDataTypeHolder impl [" + holder + "]" );
	}

	@Internal
	public static class BasicHolder implements IntegralDataTypeHolder {
		private final Class<?> exactType;
		private long value = Long.MIN_VALUE;

		public BasicHolder(Class<?> exactType) {
			this.exactType = exactType;
			if ( exactType != Long.class && exactType != Integer.class && exactType != Short.class ) {
				throw new IdentifierGenerationException( "Invalid type for basic integral holder : " + exactType );
			}
		}

		public long getActualLongValue() {
			return value;
		}

		public IntegralDataTypeHolder initialize(long value) {
			this.value = value;
			return this;
		}

		public IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException {
			long value = resultSet.getLong( 1 );
			if ( resultSet.wasNull() ) {
				value = defaultValue;
			}
			return initialize( value );
		}

		public void bind(PreparedStatement preparedStatement, int position) throws SQLException {
			// TODO : bind it as 'exact type'?  Not sure if that gains us anything...
			LOG.tracef( "binding parameter [%s] - [%s]", position, value );
			preparedStatement.setLong( position, value );
		}

		public IntegralDataTypeHolder increment() {
			checkInitialized();
			value++;
			return this;
		}

		private void checkInitialized() {
			if ( value == Long.MIN_VALUE ) {
				throw new IdentifierGenerationException( "integral holder was not initialized" );
			}
		}

		public IntegralDataTypeHolder add(long addend) {
			checkInitialized();
			value += addend;
			return this;
		}

		public IntegralDataTypeHolder decrement() {
			checkInitialized();
			value--;
			return this;
		}

		public IntegralDataTypeHolder subtract(long subtrahend) {
			checkInitialized();
			value -= subtrahend;
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor) {
			return multiplyBy( extractLong( factor ) );
		}

		public IntegralDataTypeHolder multiplyBy(long factor) {
			checkInitialized();
			value *= factor;
			return this;
		}

		public boolean eq(IntegralDataTypeHolder other) {
			return eq( extractLong( other ) );
		}

		public boolean eq(long value) {
			checkInitialized();
			return this.value == value;
		}

		public boolean lt(IntegralDataTypeHolder other) {
			return lt( extractLong( other ) );
		}

		public boolean lt(long value) {
			checkInitialized();
			return this.value < value;
		}

		public boolean gt(IntegralDataTypeHolder other) {
			return gt( extractLong( other ) );
		}

		public boolean gt(long value) {
			checkInitialized();
			return this.value > value;
		}

		public IntegralDataTypeHolder copy() {
			BasicHolder copy = new BasicHolder( exactType );
			copy.value = value;
			return copy;
		}

		public Number makeValue() {
			// TODO : should we check for truncation?
			checkInitialized();
			if ( exactType == Long.class ) {
				return value;
			}
			else if ( exactType == Integer.class ) {
				return (int) value;
			}
			else {
				return (short) value;
			}
		}

		public Number makeValueThenIncrement() {
			final Number result = makeValue();
			value++;
			return result;
		}

		public Number makeValueThenAdd(long addend) {
			final Number result = makeValue();
			value += addend;
			return result;
		}

		@Override
		public String toString() {
			return "BasicHolder[" + exactType.getName() + "[" + value + "]]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			BasicHolder that = (BasicHolder) o;

			return value == that.value;
		}

		@Override
		public int hashCode() {
			return (int) ( value ^ ( value >>> 32 ) );
		}
	}

	public static class BigIntegerHolder implements IntegralDataTypeHolder {
		private BigInteger value;

		public IntegralDataTypeHolder initialize(long value) {
			this.value = BigInteger.valueOf( value );
			return this;
		}

		public IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException {
			final BigDecimal rsValue = resultSet.getBigDecimal( 1 );
			if ( resultSet.wasNull() ) {
				return initialize( defaultValue );
			}
			this.value = rsValue.setScale( 0, RoundingMode.UNNECESSARY ).toBigInteger();
			return this;
		}

		public void bind(PreparedStatement preparedStatement, int position) throws SQLException {
			preparedStatement.setBigDecimal( position, new BigDecimal( value ) );
		}

		public IntegralDataTypeHolder increment() {
			checkInitialized();
			value = value.add( BigInteger.ONE );
			return this;
		}

		private void checkInitialized() {
			if ( value == null ) {
				throw new IdentifierGenerationException( "integral holder was not initialized" );
			}
		}

		public IntegralDataTypeHolder add(long increment) {
			checkInitialized();
			value = value.add( BigInteger.valueOf( increment ) );
			return this;
		}

		public IntegralDataTypeHolder decrement() {
			checkInitialized();
			value = value.subtract( BigInteger.ONE );
			return this;
		}

		public IntegralDataTypeHolder subtract(long subtrahend) {
			checkInitialized();
			value = value.subtract( BigInteger.valueOf( subtrahend ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor) {
			checkInitialized();
			value = value.multiply( extractBigInteger( factor ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(long factor) {
			checkInitialized();
			value = value.multiply( BigInteger.valueOf( factor ) );
			return this;
		}

		public boolean eq(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigInteger( other ) ) == 0;
		}

		public boolean eq(long value) {
			checkInitialized();
			return this.value.compareTo( BigInteger.valueOf( value ) ) == 0;
		}

		public boolean lt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigInteger( other ) ) < 0;
		}

		public boolean lt(long value) {
			checkInitialized();
			return this.value.compareTo( BigInteger.valueOf( value ) ) < 0;
		}

		public boolean gt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigInteger( other ) ) > 0;
		}

		public boolean gt(long value) {
			checkInitialized();
			return this.value.compareTo( BigInteger.valueOf( value ) ) > 0;
		}

		public IntegralDataTypeHolder copy() {
			BigIntegerHolder copy = new BigIntegerHolder();
			copy.value = value;
			return copy;
		}

		public Number makeValue() {
			checkInitialized();
			return value;
		}

		public Number makeValueThenIncrement() {
			final Number result = makeValue();
			value = value.add( BigInteger.ONE );
			return result;
		}

		public Number makeValueThenAdd(long addend) {
			final Number result = makeValue();
			value = value.add( BigInteger.valueOf( addend ) );
			return result;
		}

		@Override
		public String toString() {
			return "BigIntegerHolder[" + value + "]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			BigIntegerHolder that = (BigIntegerHolder) o;

			return Objects.equals( value, that.value );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( value );
		}
	}

	public static class BigDecimalHolder implements IntegralDataTypeHolder {
		private BigDecimal value;

		public IntegralDataTypeHolder initialize(long value) {
			this.value = BigDecimal.valueOf( value );
			return this;
		}

		public IntegralDataTypeHolder initialize(ResultSet resultSet, long defaultValue) throws SQLException {
			final BigDecimal rsValue = resultSet.getBigDecimal( 1 );
			if ( resultSet.wasNull() ) {
				return initialize( defaultValue );
			}
			this.value = rsValue.setScale( 0, RoundingMode.UNNECESSARY );
			return this;
		}

		public void bind(PreparedStatement preparedStatement, int position) throws SQLException {
			preparedStatement.setBigDecimal( position, value );
		}

		public IntegralDataTypeHolder increment() {
			checkInitialized();
			value = value.add( BigDecimal.ONE );
			return this;
		}

		private void checkInitialized() {
			if ( value == null ) {
				throw new IdentifierGenerationException( "integral holder was not initialized" );
			}
		}

		public IntegralDataTypeHolder add(long increment) {
			checkInitialized();
			value = value.add( BigDecimal.valueOf( increment ) );
			return this;
		}

		public IntegralDataTypeHolder decrement() {
			checkInitialized();
			value = value.subtract( BigDecimal.ONE );
			return this;
		}

		public IntegralDataTypeHolder subtract(long subtrahend) {
			checkInitialized();
			value = value.subtract( BigDecimal.valueOf( subtrahend ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(IntegralDataTypeHolder factor) {
			checkInitialized();
			value = value.multiply( extractBigDecimal( factor ) );
			return this;
		}

		public IntegralDataTypeHolder multiplyBy(long factor) {
			checkInitialized();
			value = value.multiply( BigDecimal.valueOf( factor ) );
			return this;
		}

		public boolean eq(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigDecimal( other ) ) == 0;
		}

		public boolean eq(long value) {
			checkInitialized();
			return this.value.compareTo( BigDecimal.valueOf( value ) ) == 0;
		}

		public boolean lt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigDecimal( other ) ) < 0;
		}

		public boolean lt(long value) {
			checkInitialized();
			return this.value.compareTo( BigDecimal.valueOf( value ) ) < 0;
		}

		public boolean gt(IntegralDataTypeHolder other) {
			checkInitialized();
			return value.compareTo( extractBigDecimal( other ) ) > 0;
		}

		public boolean gt(long value) {
			checkInitialized();
			return this.value.compareTo( BigDecimal.valueOf( value ) ) > 0;
		}

		public IntegralDataTypeHolder copy() {
			BigDecimalHolder copy = new BigDecimalHolder();
			copy.value = value;
			return copy;
		}

		public Number makeValue() {
			checkInitialized();
			return value;
		}

		public Number makeValueThenIncrement() {
			final Number result = makeValue();
			value = value.add( BigDecimal.ONE );
			return result;
		}

		public Number makeValueThenAdd(long addend) {
			final Number result = makeValue();
			value = value.add( BigDecimal.valueOf( addend ) );
			return result;
		}

		@Override
		public String toString() {
			return "BigDecimalHolder[" + value + "]";
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			BigDecimalHolder that = (BigDecimalHolder) o;

			return Objects.equals( this.value, that.value );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( value );
		}
	}

	/**
	 * Disallow instantiation of IdentifierGeneratorHelper.
	 */
	private IdentifierGeneratorHelper() {
	}
}
