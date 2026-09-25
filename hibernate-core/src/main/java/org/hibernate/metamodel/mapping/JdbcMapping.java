package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.query.sqm.CastType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Describes the mapping for things which can be expressed in a SQL query.
 * <p>
 * Generally speaking this models a column.  However, it can also model SQL
 * tuples as well
 * <p>
 * This includes details such as
 * <ul>
 *     <li>
 *         the {@linkplain #getJavaTypeDescriptor() Java type} of the mapping
 *     </li>
 *     <li>
 *         the {@linkplain #getJdbcType() JDBC type} of the mapping
 *     </li>
 *     <li>
 *         how to {@linkplain #getJdbcValueExtractor() read} values
 *         from {@linkplain java.sql.ResultSet result-sets}
 *         as well as {@linkplain java.sql.CallableStatement callable parameters}
 *     </li>
 *     <li>
 *         how to {@linkplain #getJdbcValueBinder() write} values to
 *         {@linkplain java.sql.PreparedStatement JDBC statements}
 *     </li>
 * </ul>
 * <p>
 * Some mappings will have an associated {@linkplain #getValueConverter() value converter}.
 * The {@linkplain #getJdbcValueExtractor() readers} and {@linkplain #getJdbcValueBinder() writers}
 * for such mappings will already incorporate those conversions
 * <p>
 * Some mappings support usage as SQL literals.  Such mappings will return a non-null
 * {@linkplain #getJdbcLiteralFormatter literal formatter} which handles formatting
 * values as a SQL literal
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface JdbcMapping extends MappingType, JdbcMappingContainer {
	/**
	 * The descriptor for the Java type represented by this
	 * expressible type
	 */
	@Nonnull
	JavaType<?> getJavaTypeDescriptor();

	/**
	 * The descriptor for the SQL type represented by this
	 * expressible type
	 */
	@Nonnull
	JdbcType getJdbcType();

	/**
	 * The strategy for extracting values of this expressible
	 * type from JDBC ResultSets, CallableStatements, etc
	 */
	@Nonnull
	ValueExtractor<?> getJdbcValueExtractor();

	/**
	 * The strategy for binding values of this expressible type to
	 * JDBC {@code PreparedStatement}s and {@code CallableStatement}s.
	 */
	@Nonnull
	ValueBinder getJdbcValueBinder();

	@Nonnull
	default CastType getCastType() {
		return getJdbcType().getCastType();
	}

	/**
	 * The strategy for formatting values of this expressible type to
	 * a SQL literal.
	 */
	@Nullable
	@Incubating(since = "5.4")
	default JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return getJdbcType().getJdbcLiteralFormatter( getMappedJavaType() );
	}

	@Nonnull
	@Override
	default JavaType<?> getMappedJavaType() {
		return getJavaTypeDescriptor();
	}

	@Nonnull
	@Incubating(since = "5.4")
	default JavaType<?> getJdbcJavaType() {
		return getJavaTypeDescriptor();
	}

	/**
	 * Returns the converter that this basic type uses for transforming from the domain type, to the relational type,
	 * or <code>null</code> if there is no conversion.
	 */
	@Nullable
	@Incubating(since = "5.4")
	default BasicValueConverter<?,?> getValueConverter() {
		return null;
	}

	//TODO: would it be better to just give JdbcMapping a
	//      noop converter by default, instead of having
	//      to deal with null here?
	@Nullable
	default <T> Object convertToRelationalValue(@Nullable T value) {
		final var converter = getValueConverter();
		if ( converter == null ) {
			return value;
		}
		else {
			assert value == null
				|| converter.getDomainJavaType().isInstance( value );
			@SuppressWarnings( "unchecked" ) // safe, we just checked
			final var valueConverter = (BasicValueConverter<T,?>) converter;
			return valueConverter.toRelationalValue( value );
		}
	}

	@Nullable
	default <T> Object convertToDomainValue(@Nullable T value) {
		var converter = getValueConverter();
		if ( converter == null ) {
			return value;
		}
		else {
			assert value == null
				|| converter.getRelationalJavaType().isInstance( value );
			@SuppressWarnings( "unchecked" ) // safe, we just checked
			final var valueConverter = (BasicValueConverter<?, T>) converter;
			return valueConverter.toDomainValue( value );
		}
	}

	@Override
	default int getJdbcTypeCount() {
		return 1;
	}

	@Nonnull
	@Override
	default JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return this;
	}

	@Nonnull
	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return this;
	}

	@Override
	default int forEachJdbcType(@Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, this );
		return 1;
	}

	@Override
	default int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, this );
		return 1;
	}
}
