/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.Internal;
import org.hibernate.TransientObjectException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.cfg.MappingSettings.ID_DB_STRUCTURE_NAMING_STRATEGY;
import static org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved;
import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;
import static org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY;

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

	public static Object getForeignId(
			String entityName, String propertyName, SharedSessionContractImplementor sessionImplementor, Object object) {
		final EntityPersister entityDescriptor =
				sessionImplementor.getFactory().getMappingMetamodel()
						.getEntityDescriptor( entityName );
		if ( sessionImplementor.isSessionImplementor()
				&& sessionImplementor.asSessionImplementor().contains( entityName, object ) ) {
			//abort the save (the object is already saved by a circular cascade)
			return SHORT_CIRCUIT_INDICATOR;
			//throw new IdentifierGenerationException("save associated object first, or disable cascade for inverse association");
		}
		else {
			return identifier( sessionImplementor, entityType( propertyName, entityDescriptor ),
					associatedEntity( entityName, propertyName, object, entityDescriptor ) );
		}
	}

	private static Object associatedEntity(
			String entityName, String propertyName, Object object, EntityPersister entityDescriptor) {
		final Object associatedObject = entityDescriptor.getPropertyValue( object, propertyName );
		if ( associatedObject == null ) {
			throw new IdentifierGenerationException( "Could not assign id from null association '" + propertyName
					+ "' of entity '" + entityName + "'" );
		}
		return associatedObject;
	}

	private static Object identifier(
			SharedSessionContractImplementor sessionImplementor,
			EntityType foreignValueSourceType,
			Object associatedEntity) {
		final String associatedEntityName = foreignValueSourceType.getAssociatedEntityName();
		try {
			return getEntityIdentifierIfNotUnsaved( associatedEntityName, associatedEntity, sessionImplementor );
		}
		catch (TransientObjectException toe) {
			if ( sessionImplementor.isSessionImplementor() ) {
				sessionImplementor.asSessionImplementor().persist( associatedEntityName, associatedEntity );
				return sessionImplementor.getContextEntityIdentifier( associatedEntity );
			}
			else if ( sessionImplementor.isStatelessSession() ) {
				return sessionImplementor.asStatelessSession().insert( associatedEntityName, associatedEntity );
			}
			else {
				throw new IdentifierGenerationException("sessionImplementor is neither Session nor StatelessSession");
			}
		}
	}

	private static EntityType entityType(String propertyName, EntityPersister entityDescriptor) {
		final Type propertyType = entityDescriptor.getPropertyType( propertyName );
		if ( propertyType instanceof EntityType ) {
			// the normal case
			return (EntityType) propertyType;
		}
		else {
			// try identifier mapper
			final String mapperPropertyName = IDENTIFIER_MAPPER_PROPERTY + "." + propertyName;
			return (EntityType) entityDescriptor.getPropertyType( mapperPropertyName );
		}
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
			return Long.hashCode(value);
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

	public static ImplicitDatabaseObjectNamingStrategy getNamingStrategy(Properties params, ServiceRegistry serviceRegistry) {
		final StrategySelector strategySelector = serviceRegistry.requireService( StrategySelector.class );

		final String namingStrategySetting = coalesceSuppliedValues(
				() -> {
					final String localSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, params );
					if ( localSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return localSetting;
				},
				() -> {
					final ConfigurationService configurationService = serviceRegistry.requireService( ConfigurationService.class );
					final String globalSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, configurationService.getSettings() );
					if ( globalSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return globalSetting;
				},
				StandardNamingStrategy.class::getName
		);

		return strategySelector.resolveStrategy( ImplicitDatabaseObjectNamingStrategy.class, namingStrategySetting );
	}

	/**
	 * Disallow instantiation of IdentifierGeneratorHelper.
	 */
	private IdentifierGeneratorHelper() {
	}
}
