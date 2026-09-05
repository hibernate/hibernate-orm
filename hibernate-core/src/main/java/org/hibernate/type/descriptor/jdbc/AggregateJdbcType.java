/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.SQLException;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.WrapperOptions;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes aggregate handling such as [org.hibernate.type.SqlTypes#STRUCT],
/// [org.hibernate.type.SqlTypes#JSON], and
/// [org.hibernate.type.SqlTypes#SQLXML].
///
/// A prototype descriptor supplies its mapping-specific descriptor through
/// [#resolveAggregateJdbcType(EmbeddableMappingType, String, RuntimeModelCreationContext)].
///
/// @see #resolveAggregateJdbcType(EmbeddableMappingType, String, RuntimeModelCreationContext)
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface AggregateJdbcType extends JdbcType {

	/// Resolves the registered prototype represented by this instance into the
	/// operational descriptor for one mapped aggregate.
	///
	/// The instance registered with Hibernate usually represents only the native
	/// aggregate format. At that stage, the aggregate's embeddable mapping, SQL
	/// type name, and any mapping-specific component ordering are not yet known.
	/// During runtime-model creation, Hibernate calls this method with that
	/// information and uses the returned descriptor for binding and extraction.
	///
	/// Implementations which depend on the aggregate mapping should return a new
	/// immutable or thread-safe descriptor retaining the required mapping-specific
	/// state. A genuinely mapping-independent implementation may return `this`.
	/// The returned descriptor need not have the same implementation class as the
	/// registered prototype.
	///
	/// @param mappingType the embeddable mapping represented by the aggregate
	/// @param sqlType the aggregate's SQL type name
	/// @param creationContext access to the boot and runtime-model creation state
	/// @return the aggregate JDBC type descriptor to use for this mapping
	///
	/// @see AggregateJdbcType
	/// @see StructuredJdbcType
	@SPI(SUPPLY)
	AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext);

	/// Return the embeddable mapping represented by this aggregate JDBC type.
	///
	/// A mapping-specific descriptor returned by
	/// [#resolveAggregateJdbcType(EmbeddableMappingType, String, RuntimeModelCreationContext)]
	/// returns the mapping supplied during resolution. A registered prototype, a
	/// genuinely mapping-independent descriptor, or a descriptor used to read an
	/// untyped native value may return `null`.
	///
	/// Consumers which require mapping metadata must use a resolved descriptor.
	///
	/// @return the represented embeddable mapping, or `null` when this descriptor
	/// is not associated with a particular mapping
	@Nullable EmbeddableMappingType getEmbeddableMappingType();

	/// Create the single driver-bindable aggregate representation of a non-null
	/// mapped domain value.
	///
	/// Implementations whose native representation is assembled from component
	/// values should use
	/// [org.hibernate.type.descriptor.jdbc.spi.AggregateJdbcValues#fromDomainValue(EmbeddableMappingType, Object, WrapperOptions)].
	/// Aggregate binders handle SQL null before invoking this method.
	///
	/// @param domainValue the non-null mapped aggregate value
	/// @param options the options governing JDBC value conversion
	///
	/// @return the single value to pass to the JDBC driver
	Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException;

	/// Extract the component JDBC values from a native aggregate representation.
	///
	/// The returned values must use Hibernate's logical mapping order, even when
	/// the native representation uses a different physical order. They are JDBC
	/// component representations, not mapped domain attribute values. Implementations
	/// may use
	/// [org.hibernate.type.descriptor.jdbc.spi.AggregateJdbcValues#toLogicalJdbcValues(EmbeddableMappingType, Object[], WrapperOptions)]
	/// after decoding the native container.
	///
	/// @param rawJdbcValue the native aggregate value obtained from the driver
	/// @param options the options governing JDBC value conversion
	///
	/// @return the logically ordered component JDBC values, or `null` when the
	/// native value represents SQL null
	Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException;
}
