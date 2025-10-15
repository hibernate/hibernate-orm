/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Incubating;
import org.hibernate.internal.util.IndexedConsumer;
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
public interface JdbcMapping extends MappingType, JdbcMappingContainer {
	/**
	 * The descriptor for the Java type represented by this
	 * expressible type
	 */
	JavaType<?> getJavaTypeDescriptor();

	/**
	 * The descriptor for the SQL type represented by this
	 * expressible type
	 */
	JdbcType getJdbcType();

	/**
	 * The strategy for extracting values of this expressible
	 * type from JDBC ResultSets, CallableStatements, etc
	 */
	ValueExtractor<?> getJdbcValueExtractor();

	/**
	 * The strategy for binding values of this expressible type to
	 * JDBC {@code PreparedStatement}s and {@code CallableStatement}s.
	 */
	ValueBinder getJdbcValueBinder();

	default CastType getCastType() {
		return getJdbcType().getCastType();
	}

	/**
	 * The strategy for formatting values of this expressible type to
	 * a SQL literal.
	 */
	@Incubating
	default JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return getJdbcType().getJdbcLiteralFormatter( getMappedJavaType() );
	}

	@Override
	default JavaType<?> getMappedJavaType() {
		return getJavaTypeDescriptor();
	}

	@Incubating
	default JavaType<?> getJdbcJavaType() {
		return getJavaTypeDescriptor();
	}

	/**
	 * Returns the converter that this basic type uses for transforming from the domain type, to the relational type,
	 * or <code>null</code> if there is no conversion.
	 */
	@Incubating
	default BasicValueConverter<?,?> getValueConverter() {
		return null;
	}

	//TODO: would it be better to just give JdbcMapping a
	//      noop converter by default, instead of having
	//      to deal with null here?
	default <T> Object convertToRelationalValue(T value) {
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

	default <T> Object convertToDomainValue(T value) {
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

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( index );
		}
		return this;
	}

	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return this;
	}

	@Override
	default int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, this );
		return 1;
	}

	@Override
	default int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, this );
		return 1;
	}
}
