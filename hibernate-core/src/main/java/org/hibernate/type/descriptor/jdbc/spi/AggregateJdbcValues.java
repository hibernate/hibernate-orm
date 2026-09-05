/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.StructHelper;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Converts aggregate values between their mapped-domain representation and
/// the component JDBC values consumed or produced by an
/// [org.hibernate.type.descriptor.jdbc.AggregateJdbcType].
///
/// A provider remains responsible for encoding and decoding its native
/// aggregate container. After decoding, pass its component values to
/// [#toLogicalJdbcValues(EmbeddableMappingType, Object[], AggregateJdbcValueOrder, WrapperOptions)]
/// or [#toDomainValue(EmbeddableMappingType, Object[], AggregateJdbcValueOrder, WrapperOptions)].
/// Before encoding, obtain physical component values with
/// [#fromDomainValue(EmbeddableMappingType, Object, AggregateJdbcValueOrder, WrapperOptions)].
///
/// The methods accepting no [AggregateJdbcValueOrder] use the logical mapping
/// order. None of these methods modifies an array supplied by the caller.
///
/// @since 8.0
///
/// @author Steve Ebersole
/// @author Christian Beikov
@Incubating
@SPI(USE)
public final class AggregateJdbcValues {
	private AggregateJdbcValues() {
	}

	/// Decompose a non-null mapped aggregate value into component JDBC values in
	/// logical mapping order.
	///
	/// @param mappingType the mapped aggregate type
	/// @param domainValue the non-null mapped aggregate value
	/// @param options the options governing JDBC value conversion
	/// @return a new array containing the component JDBC values
	public static Object[] fromDomainValue(
			EmbeddableMappingType mappingType,
			Object domainValue,
			WrapperOptions options) throws SQLException {
		return fromDomainValue( mappingType, domainValue, AggregateJdbcValueOrder.identity(), options );
	}

	/// Decompose a non-null mapped aggregate value into component JDBC values in
	/// the physical order described by `order`.
	///
	/// @param mappingType the mapped aggregate type
	/// @param domainValue the non-null mapped aggregate value
	/// @param order the native container's physical component order
	/// @param options the options governing JDBC value conversion
	/// @return a new array containing the physically ordered component JDBC values
	public static Object[] fromDomainValue(
			EmbeddableMappingType mappingType,
			Object domainValue,
			AggregateJdbcValueOrder order,
			WrapperOptions options) throws SQLException {
		requireNonNull( mappingType, "mappingType" );
		requireNonNull( domainValue, "domainValue" );
		requireNonNull( order, "order" );
		requireNonNull( options, "options" );
		final int valueCount = valueCount( mappingType );
		return StructHelper.getJdbcValues(
				mappingType,
				order.physicalOrder( valueCount ),
				domainValue,
				options
		);
	}

	/// Convert decoded component values in logical mapping order to the JDBC
	/// component representations expected by Hibernate.
	///
	/// @param mappingType the mapped aggregate type
	/// @param rawPhysicalValues the decoded values, already in logical order
	/// @param options the options governing JDBC value conversion
	/// @return a new array containing converted values in logical mapping order
	public static Object[] toLogicalJdbcValues(
			EmbeddableMappingType mappingType,
			Object[] rawPhysicalValues,
			WrapperOptions options) throws SQLException {
		return toLogicalJdbcValues(
				mappingType,
				rawPhysicalValues,
				AggregateJdbcValueOrder.identity(),
				options
		);
	}

	/// Convert decoded component values in physical order to the JDBC component
	/// representations expected by Hibernate in logical mapping order.
	///
	/// Driver-specific container decoding and scalar normalization occur before
	/// this method is called.
	///
	/// @param mappingType the mapped aggregate type
	/// @param rawPhysicalValues the values decoded from the native container
	/// @param order the native container's physical component order
	/// @param options the options governing JDBC value conversion
	/// @return a new array containing converted values in logical mapping order
	///
	/// @throws IllegalArgumentException if the value or ordering count does not
	/// match the aggregate mapping
	public static Object[] toLogicalJdbcValues(
			EmbeddableMappingType mappingType,
			Object[] rawPhysicalValues,
			AggregateJdbcValueOrder order,
			WrapperOptions options) throws SQLException {
		validateExtractionArguments( mappingType, rawPhysicalValues, order, options );
		return StructHelper.toLogicalJdbcValues(
				mappingType,
				order.logicalOrder( rawPhysicalValues.length ),
				rawPhysicalValues,
				options
		);
	}

	/// Convert decoded component values in logical mapping order and instantiate
	/// the mapped aggregate domain value.
	///
	/// @param mappingType the mapped aggregate type
	/// @param rawPhysicalValues the decoded values, already in logical order
	/// @param options the options governing JDBC value conversion
	/// @return the instantiated mapped aggregate value
	public static Object toDomainValue(
			EmbeddableMappingType mappingType,
			Object[] rawPhysicalValues,
			WrapperOptions options) throws SQLException {
		return toDomainValue(
				mappingType,
				rawPhysicalValues,
				AggregateJdbcValueOrder.identity(),
				options
		);
	}

	/// Convert decoded component values in physical order and instantiate the
	/// mapped aggregate domain value.
	///
	/// Driver-specific container decoding and scalar normalization occur before
	/// this method is called.
	///
	/// @param mappingType the mapped aggregate type
	/// @param rawPhysicalValues the values decoded from the native container
	/// @param order the native container's physical component order
	/// @param options the options governing JDBC value conversion
	/// @return the instantiated mapped aggregate value
	///
	/// @throws IllegalArgumentException if the value or ordering count does not
	/// match the aggregate mapping
	public static Object toDomainValue(
			EmbeddableMappingType mappingType,
			Object[] rawPhysicalValues,
			AggregateJdbcValueOrder order,
			WrapperOptions options) throws SQLException {
		validateExtractionArguments( mappingType, rawPhysicalValues, order, options );
		return StructHelper.toDomainValue(
				mappingType,
				order.physicalOrder( rawPhysicalValues.length ),
				rawPhysicalValues,
				options
		);
	}

	private static void validateExtractionArguments(
			EmbeddableMappingType mappingType,
			Object[] rawPhysicalValues,
			AggregateJdbcValueOrder order,
			WrapperOptions options) {
		requireNonNull( mappingType, "mappingType" );
		requireNonNull( rawPhysicalValues, "rawPhysicalValues" );
		requireNonNull( order, "order" );
		requireNonNull( options, "options" );
		final int expectedValueCount = valueCount( mappingType );
		if ( rawPhysicalValues.length != expectedValueCount ) {
			throw new IllegalArgumentException(
					"Aggregate JDBC-value array has " + rawPhysicalValues.length
							+ " values, but the aggregate mapping has " + expectedValueCount
			);
		}
		order.physicalOrder( expectedValueCount );
	}

	private static int valueCount(EmbeddableMappingType mappingType) {
		return mappingType.getJdbcValueCount() + ( mappingType.isPolymorphic() ? 1 : 0 );
	}
}
