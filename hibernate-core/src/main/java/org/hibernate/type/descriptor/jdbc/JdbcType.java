/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.SQLException;

import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.sqm.CastType;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.*;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes the SQL/JDBC side of a value mapping. A `JdbcType` is coupled
/// with a [JavaType] to describe how a Java value is read from and written to
/// JDBC.
///
/// A descriptor need not correspond directly to a database column type. Its
/// JDBC and DDL type codes identify the relevant [java.sql.Types] and
/// [org.hibernate.type.SqlTypes]
/// semantics, while its supplied [ValueBinder], [ValueExtractor], and optional
/// [JdbcLiteralFormatter] perform the JDBC operations.
///
/// Providers may implement this contract and contribute a stable descriptor
/// through [org.hibernate.boot.model.TypeContributions] or
/// [org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry]. Do not expose an
/// internal implementation class from provider code; use supported contracts
/// and Dialect type facilities.
///
/// A mapping may select a descriptor using
/// [org.hibernate.annotations.JdbcType] or indirectly using
/// [org.hibernate.annotations.JdbcTypeCode].
///
/// @see org.hibernate.boot.model.TypeContributions#contributeJdbcType(JdbcType)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addDescriptor(JdbcType)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addDescriptor(int, JdbcType)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addDescriptorIfAbsent(JdbcType)
/// @see org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry#addDescriptorIfAbsent(int, JdbcType)
/// @see JavaType#getRecommendedJdbcType(JdbcTypeIndicators)
/// @see AdjustableJdbcType#resolveIndicatedType(JdbcTypeIndicators, JavaType)
/// @see AggregateJdbcType#resolveAggregateJdbcType(org.hibernate.metamodel.mapping.EmbeddableMappingType, String, org.hibernate.metamodel.spi.RuntimeModelCreationContext)
/// @see JdbcTypeConstructor#resolveType(TypeConfiguration, Dialect, org.hibernate.type.BasicType, org.hibernate.tool.schema.extract.spi.ColumnTypeInformation)
/// @see JdbcTypeConstructor#resolveType(TypeConfiguration, Dialect, JdbcType, org.hibernate.tool.schema.extract.spi.ColumnTypeInformation)
/// @see org.hibernate.annotations.AnyKeyJdbcType#value()
/// @see org.hibernate.annotations.CollectionIdJdbcType#value()
/// @see org.hibernate.annotations.JdbcType#value()
/// @see org.hibernate.annotations.JdbcTypeRegistration#value()
/// @see org.hibernate.annotations.ListIndexJdbcType#value()
/// @see org.hibernate.annotations.MapKeyJdbcType#value()
/// @see org.hibernate.mapping.BasicValue#setExplicitJdbcTypeAccess(java.util.function.Function)
///
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface JdbcType extends Serializable {
	/**
	 * A "friendly" name for use in logging
	 */
	default String getFriendlyName() {
		return Integer.toString( getDefaultSqlTypeCode() );
	}

	/**
	 * The {@linkplain SqlTypes JDBC type code} used when interacting with JDBC APIs.
	 * <p>
	 * For example, it's used when calling {@link java.sql.PreparedStatement#setNull(int, int)}.
	 *
	 * @return a JDBC type code
	 */
	int getJdbcTypeCode();

	/**
	 * A {@linkplain SqlTypes JDBC type code} that identifies the SQL column type.
	 * <p>
	 * This value might be different from {@link #getDdlTypeCode()} if the actual type
	 * e.g. JSON is emulated through a type like CLOB.
	 *
	 * @return a JDBC type code
	 */
	default int getDefaultSqlTypeCode() {
		return getJdbcTypeCode();
	}

	/**
	 * A {@linkplain SqlTypes JDBC type code} that identifies the SQL column type
	 * used for schema generation.
	 * <p>
	 * This value is passed to {@link DdlTypeRegistry#getTypeName(int, Size, Type)}
	 * to obtain the SQL column type.
	 *
	 * @return a JDBC type code
	 * @since 6.2
	 */
	default int getDdlTypeCode() {
		return getDefaultSqlTypeCode();
	}

	/**
	 * The {@linkplain JavaType Java type} usually is used to represent values of
	 * this JDBC type in the entity model of the data. Often, but not always, the
	 * source of this recommendation is the JDBC specification.
	 *
	 * @since 7.2
	 */
	default JavaType<?> getRecommendedJavaType(
			Integer precision, Integer scale,
			TypeConfiguration typeConfiguration) {
		// match legacy behavior
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
				JdbcTypeJavaClassMappings.INSTANCE.determineJavaClassForJdbcTypeCode( getDefaultSqlTypeCode() )
		);
	}

	/// Supply a formatter capable of rendering values of the given Java type as
	/// SQL literals of this JDBC type.
	///
	/// @see JdbcLiteralFormatter
	// todo (6.0) : move to {@link org.hibernate.metamodel.mapping.JdbcMapping}?
	@SPI(SUPPLY)
	default <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) ->
				appender.appendSql( value.toString() );
	}

	/**
	 * Obtain a {@linkplain ValueBinder binder} object capable of binding values of the
	 * given {@linkplain JavaType Java type} to parameters of a JDBC
	 * {@link java.sql.PreparedStatement}.
	 *
	 * @param javaType The descriptor describing the types of Java values to be bound
	 *
	 * @return The appropriate binder.
	 * @see ValueBinder
	 */
	@SPI(SUPPLY)
	<X> ValueBinder<X> getBinder(JavaType<X> javaType);

	/**
	 * Obtain an {@linkplain ValueExtractor extractor} object capable of extracting
	 * values of the given {@linkplain JavaType Java type} from a JDBC
	 * {@link java.sql.ResultSet}.
	 *
	 * @param javaType The descriptor describing the types of Java values to be extracted
	 *
	 * @return The appropriate extractor
	 * @see ValueExtractor
	 */
	@SPI(SUPPLY)
	<X> ValueExtractor<X> getExtractor(JavaType<X> javaType);

	/**
	 * The Java type class that is preferred by the binder or null.
	 */
	@Incubating
	default Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return null;
	}

	/**
	 * The check constraint that should be added to the column
	 * definition in generated DDL.
	 *
	 * @param columnName the name of the column
	 * @param javaType   the {@link JavaType} of the mapped column
	 * @param converter  the converter, if any, or null
	 * @param dialect    the SQL {@link Dialect}
	 * @return a check constraint condition or null
	 * @since 6.2
	 */
	default String getCheckCondition(String columnName, JavaType<?> javaType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		return null;
	}

	/**
	 * Wraps the top level selection expression to be able to read values with this JdbcType's ValueExtractor.
	 * @since 6.2
	 */
	@Incubating
	default Expression wrapTopLevelSelectionExpression(Expression expression) {
		return expression;
	}

	/**
	 * Wraps the write expression to be able to write values with this JdbcType's ValueBinder.
	 * @since 7.2
	 */
	@Incubating
	default String wrapWriteExpression(String writeExpression, @Nullable Size size, Dialect dialect) {
		final var wrapped = new StringBuilder( writeExpression.length() );
		appendWriteExpression( writeExpression, size, new StringBuilderSqlAppender( wrapped ), dialect );
		return wrapped.toString();
	}

	/**
	 * Append the write expression wrapped in a way to be able to write values with this JdbcType's ValueBinder.
	 * @since 7.2
	 */
	@Incubating
	default void appendWriteExpression(String writeExpression, @Nullable Size size, SqlAppender appender, Dialect dialect) {
		appender.appendSql( writeExpression );
	}

	/**
	 * Whether the write expression is typed.
	 * This is used to determine if a parameter expression needs a cast in e.g. a select item context.
	 * @since 7.2
	 */
	@Incubating
	default boolean isWriteExpressionTyped(Dialect dialect) {
		return false;
	}

	default boolean isInteger() {
		final int typeCode = getDdlTypeCode();
		return isIntegral(typeCode)
			|| typeCode == BIT; //HIGHLY DUBIOUS!
	}

	default boolean isFloat() {
		return isFloatOrRealOrDouble( getDdlTypeCode() );
	}

	default boolean isDecimal() {
		return isNumericOrDecimal( getDdlTypeCode() );
	}

	default boolean isNumber() {
		return isNumericType( getDdlTypeCode() );
	}

	default boolean isBinary() {
		return isBinaryType( getDdlTypeCode() );
	}

	default boolean isString() {
		return isCharacterOrClobType( getDdlTypeCode() );
	}

	default boolean isStringLike() {
		final int ddlTypeCode = getDdlTypeCode();
		return isCharacterOrClobType( ddlTypeCode )
			|| isEnumType( ddlTypeCode );
	}

	default boolean isTemporal() {
		return isTemporalType( getDdlTypeCode() );
	}

	default boolean isLob() {
		return isLob( getDdlTypeCode() );
	}

	static boolean isLob(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case BLOB:
			case CLOB:
			case NCLOB: {
				return true;
			}
		}
		return false;
	}

	default boolean isLobOrLong() {
		return isLobOrLong( getDdlTypeCode() );
	}

	static boolean isLobOrLong(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case BLOB:
			case CLOB:
			case NCLOB:
			case LONG32VARBINARY:
			case LONG32VARCHAR:
			case LONG32NVARCHAR: {
				return true;
			}
		}
		return false;
	}

	default boolean isNationalized() {
		return isNationalized( getDdlTypeCode() );
	}

	static boolean isNationalized(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case NCHAR:
			case NVARCHAR:
			case LONGNVARCHAR:
			case LONG32NVARCHAR:
			case NCLOB: {
				return true;
			}
		}
		return false;
	}

	default boolean isInterval() {
		return isIntervalType( getDdlTypeCode() );
	}

	default boolean isDuration() {
		final int ddlTypeCode = getDefaultSqlTypeCode();
		return isDurationType( ddlTypeCode )
			|| isIntervalType( ddlTypeCode );
	}

	default boolean isArray() {
		return isArray( getDdlTypeCode() );
	}

	static boolean isArray(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case ARRAY:
			case STRUCT_ARRAY:
			case JSON_ARRAY:
			case XML_ARRAY:
				return true;
		}
		return false;
	}

	default CastType getCastType() {
		final CastType sqlTypeCodeCastType = getCastType( getDefaultSqlTypeCode() );
		return sqlTypeCodeCastType == CastType.OTHER ? getCastType( getDdlTypeCode() ) : sqlTypeCodeCastType;
	}

	static CastType getCastType(int typeCode) {
		return switch ( typeCode ) {
			case INTEGER, TINYINT, SMALLINT -> CastType.INTEGER;
			case BIGINT -> CastType.LONG;
			case FLOAT, REAL -> CastType.FLOAT;
			case DOUBLE -> CastType.DOUBLE;
			case CHAR, NCHAR, VARCHAR, NVARCHAR, LONGVARCHAR, LONGNVARCHAR -> CastType.STRING;
			case CLOB -> CastType.CLOB;
			case BOOLEAN -> CastType.BOOLEAN;
			case DECIMAL, NUMERIC -> CastType.FIXED;
			case DATE -> CastType.DATE;
			case TIME, TIME_UTC, TIME_WITH_TIMEZONE -> CastType.TIME;
			case TIMESTAMP -> CastType.TIMESTAMP;
			case TIMESTAMP_WITH_TIMEZONE -> CastType.OFFSET_TIMESTAMP;
			case JSON, JSON_ARRAY -> CastType.JSON;
			case SQLXML, XML_ARRAY -> CastType.XML;
			case NULL -> CastType.NULL;
			default -> CastType.OTHER;
		};
	}

	/**
	 * Register the {@code OUT} parameter on the {@link CallableStatement} with the given name for this {@linkplain JdbcType}.
	 * @since 6.2
	 */
	default void registerOutParameter(CallableStatement callableStatement, String name) throws SQLException {
		callableStatement.registerOutParameter( name, getJdbcTypeCode() );
	}

	/**
	 * Register the {@code OUT} parameter on the {@link CallableStatement} with the given index for this {@linkplain JdbcType}.
	 * @since 6.2
	 */
	default void registerOutParameter(CallableStatement callableStatement, int index) throws SQLException {
		callableStatement.registerOutParameter( index, getJdbcTypeCode() );
	}

	/**
	 * Add auxiliary database objects for this {@linkplain JdbcType} to the {@link Database} object.
	 *
	 * @since 6.5
	 */
	@Incubating
	default void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			BasicValueConverter<?, ?> valueConverter,
			Size columnSize,
			Database database,
			JdbcTypeIndicators context) {
	}

	@Incubating
	default String getExtraCreateTableInfo(JavaType<?> javaType, String columnName, String tableName, Database database) {
		return "";
	}

	/**
	 * Returns the cast pattern from the given source type to this type, or {@code null} if not possible.
	 *
	 * @param sourceMapping The source type
	 * @param size The size of this target type
	 * @return The cast pattern or null
	 * @since 7.2
	 */
	@Incubating
	default @Nullable String castFromPattern(JdbcMapping sourceMapping, @Nullable Size size) {
		return null;
	}

	/**
	 * Returns the cast pattern from this type to the given target type, or {@code null} if not possible.
	 *
	 * @param targetJdbcMapping The target type
	 * @param size The size of this source type
	 * @return The cast pattern or null
	 * @since 7.2
	 */
	@Incubating
	default @Nullable String castToPattern(JdbcMapping targetJdbcMapping, @Nullable Size size) {
		return null;
	}

	@Incubating
	default boolean isComparable() {
		final int code = getDefaultSqlTypeCode();
		return isCharacterType( code )
			|| isTemporalType( code )
			|| isNumericType( code )
			|| isEnumType( code )
			// both Java and the SQL database consider
			// that false < true is a sensible thing
			|| isBoolean()
			// both Java and the database consider UUIDs
			// comparable, so go ahead and accept them
			|| code == UUID;
	}

	@Incubating
	default boolean hasDatePart() {
		return SqlTypes.hasDatePart( getDefaultSqlTypeCode() );
	}

	@Incubating
	default boolean hasTimePart() {
		return SqlTypes.hasTimePart( getDefaultSqlTypeCode() );
	}

	@Incubating
	default boolean isStringLikeExcludingClob() {
		final int code = getDefaultSqlTypeCode();
		return isCharacterType( code ) || isEnumType( code );
	}

	@Incubating
	default boolean isSpatial() {
		return isSpatialType( getDefaultSqlTypeCode() );
	}

	@Incubating
	default boolean isJson() {
		return isJsonType( getDefaultSqlTypeCode() );
	}

	@Incubating
	default boolean isImplicitJson() {
		return isImplicitJsonType( getDefaultSqlTypeCode() );
	}

	@Incubating
	default boolean isXml() {
		return isXmlType( getDefaultSqlTypeCode() );
	}

	@Incubating
	default boolean isImplicitXml() {
		return isImplicitXmlType( getDefaultSqlTypeCode() );
	}

	@Incubating
	default boolean isBoolean() {
		return getDefaultSqlTypeCode() == BOOLEAN;
	}

	@Incubating
	default boolean isSmallInteger() {
		return isSmallOrTinyInt( getDefaultSqlTypeCode() );
	}
}
