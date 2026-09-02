/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.OracleBooleanJdbcType;
import org.hibernate.dialect.type.internal.OracleEnumJdbcType;
import org.hibernate.dialect.type.internal.OracleJdbcHelper;
import org.hibernate.dialect.type.internal.OracleJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.OracleJsonJdbcType;
import org.hibernate.dialect.type.internal.OracleOrdinalEnumJdbcType;
import org.hibernate.dialect.type.internal.OracleOsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.OracleOsonJdbcType;
import org.hibernate.dialect.type.internal.OracleReflectionStructJdbcType;
import org.hibernate.dialect.type.internal.OracleXmlArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.OracleXmlJdbcType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock Oracle JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// and contribute each result with the matching JDBC descriptor or constructor
/// operation. Test [#isDriverUsable(ServiceRegistry)] before requesting a
/// driver-backed result and test [#isOsonAvailable(ServiceRegistry)] before
/// contributing the OSON descriptors.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class OracleJdbcTypes {
	private OracleJdbcTypes() {
	}

	/// Obtain Oracle's pre-23 boolean descriptor.
	public static JdbcType booleanType() {
		return OracleBooleanJdbcType.INSTANCE;
	}

	/// Obtain Oracle's XML descriptor.
	public static JdbcType xml() {
		return OracleXmlJdbcType.INSTANCE;
	}

	/// Obtain Oracle's XML-array type constructor.
	public static JdbcTypeConstructor xmlArrayConstructor() {
		return OracleXmlArrayJdbcTypeConstructor.INSTANCE;
	}

	/// Obtain the reflection-based Oracle structured descriptor used when the
	/// Oracle JDBC classes are unavailable to Hibernate's class loader.
	public static JdbcType reflectionStruct() {
		return OracleReflectionStructJdbcType.INSTANCE;
	}

	/// Obtain Oracle's native JSON descriptor.
	public static JdbcType nativeJson() {
		return OracleJsonJdbcType.INSTANCE;
	}

	/// Obtain Oracle's native JSON-array type constructor.
	public static JdbcTypeConstructor nativeJsonArrayConstructor() {
		return OracleJsonArrayJdbcTypeConstructor.NATIVE_INSTANCE;
	}

	/// Obtain Oracle's BLOB-backed JSON-array type constructor.
	public static JdbcTypeConstructor blobJsonArrayConstructor() {
		return OracleJsonArrayJdbcTypeConstructor.BLOB_INSTANCE;
	}

	/// Obtain Oracle's OSON descriptor.
	public static JdbcType oson() {
		return OracleOsonJdbcType.INSTANCE;
	}

	/// Obtain Oracle's OSON-array type constructor.
	public static JdbcTypeConstructor osonArrayConstructor() {
		return OracleOsonArrayJdbcTypeConstructor.INSTANCE;
	}

	/// Obtain Oracle's named string-enum descriptor.
	public static JdbcType enumType() {
		return OracleEnumJdbcType.INSTANCE;
	}

	/// Obtain Oracle's named ordinal-enum descriptor.
	public static JdbcType ordinalEnumType() {
		return OracleOrdinalEnumJdbcType.INSTANCE;
	}

	/// Determine whether Oracle JDBC classes are visible through the supplied
	/// service registry.
	public static boolean isDriverUsable(ServiceRegistry serviceRegistry) {
		return OracleJdbcHelper.isUsable( serviceRegistry );
	}

	/// Determine whether the Oracle OSON extension is visible through the
	/// supplied service registry.
	public static boolean isOsonAvailable(ServiceRegistry serviceRegistry) {
		return OracleJdbcHelper.isOsonAvailable( serviceRegistry );
	}

	/// Create the driver-backed Oracle structured descriptor.
	///
	/// Call this method only when [#isDriverUsable(ServiceRegistry)] returns
	/// `true`.
	public static JdbcType driverStruct(ServiceRegistry serviceRegistry) {
		return OracleJdbcHelper.getStructJdbcType( serviceRegistry );
	}

	/// Create the driver-backed Oracle array type constructor.
	///
	/// Call this method only when [#isDriverUsable(ServiceRegistry)] returns
	/// `true`.
	public static JdbcTypeConstructor driverArrayConstructor(ServiceRegistry serviceRegistry) {
		return OracleJdbcHelper.getArrayJdbcTypeConstructor( serviceRegistry );
	}

	/// Create the driver-backed Oracle nested-table type constructor.
	///
	/// Call this method only when [#isDriverUsable(ServiceRegistry)] returns
	/// `true`.
	public static JdbcTypeConstructor driverNestedTableConstructor(ServiceRegistry serviceRegistry) {
		return OracleJdbcHelper.getNestedTableJdbcTypeConstructor( serviceRegistry );
	}
}
