/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.PgJdbcHelper;
import org.hibernate.dialect.type.internal.PostgreSQLArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.PostgreSQLCastingInetJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLCastingIntervalSecondJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLCastingJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.PostgreSQLCastingJsonJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLEnumJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLOrdinalEnumJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLStructCastingJdbcType;
import org.hibernate.dialect.type.internal.PostgreSQLUUIDJdbcType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock PostgreSQL JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// and preserve the Dialect's existing registration and fallback order. Test
/// [#isDriverUsable(ServiceRegistry)] before requesting a driver-backed result;
/// use the corresponding casting result otherwise.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class PostgreSQLJdbcTypes {
	private PostgreSQLJdbcTypes() {
	}

	/// Obtain PostgreSQL's standard array type constructor.
	public static JdbcTypeConstructor arrayConstructor() {
		return PostgreSQLArrayJdbcTypeConstructor.INSTANCE;
	}

	/// Obtain PostgreSQL's named string-enum descriptor.
	public static JdbcType enumType() {
		return PostgreSQLEnumJdbcType.INSTANCE;
	}

	/// Obtain PostgreSQL's named ordinal-enum descriptor.
	public static JdbcType ordinalEnumType() {
		return PostgreSQLOrdinalEnumJdbcType.INSTANCE;
	}

	/// Obtain PostgreSQL's UUID descriptor.
	public static JdbcType uuid() {
		return PostgreSQLUUIDJdbcType.INSTANCE;
	}

	/// Obtain the casting fallback for PostgreSQL structured values.
	public static JdbcType castingStruct() {
		return PostgreSQLStructCastingJdbcType.INSTANCE;
	}

	/// Obtain the casting fallback for PostgreSQL interval-second values.
	public static JdbcType castingIntervalSecond() {
		return PostgreSQLCastingIntervalSecondJdbcType.INSTANCE;
	}

	/// Obtain the casting fallback for PostgreSQL inet values.
	public static JdbcType castingInet() {
		return PostgreSQLCastingInetJdbcType.INSTANCE;
	}

	/// Obtain the casting fallback for PostgreSQL JSON values.
	public static JdbcType castingJson() {
		return PostgreSQLCastingJsonJdbcType.JSON_INSTANCE;
	}

	/// Obtain the casting fallback for PostgreSQL JSONB values.
	public static JdbcType castingJsonb() {
		return PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE;
	}

	/// Obtain the casting fallback for PostgreSQL JSON arrays.
	public static JdbcTypeConstructor castingJsonArrayConstructor() {
		return PostgreSQLCastingJsonArrayJdbcTypeConstructor.JSON_INSTANCE;
	}

	/// Obtain the casting fallback for PostgreSQL JSONB arrays.
	public static JdbcTypeConstructor castingJsonbArrayConstructor() {
		return PostgreSQLCastingJsonArrayJdbcTypeConstructor.JSONB_INSTANCE;
	}

	/// Determine whether PostgreSQL JDBC classes are visible through the
	/// supplied service registry.
	public static boolean isDriverUsable(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.isUsable( serviceRegistry );
	}

	/// Create the driver-backed PostgreSQL structured descriptor.
	public static JdbcType driverStruct(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.getStructJdbcType( serviceRegistry );
	}

	/// Create the driver-backed PostgreSQL interval-second descriptor.
	public static JdbcType driverIntervalSecond(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.getIntervalJdbcType( serviceRegistry );
	}

	/// Create the driver-backed PostgreSQL inet descriptor.
	public static JdbcType driverInet(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.getInetJdbcType( serviceRegistry );
	}

	/// Create the driver-backed PostgreSQL JSON descriptor.
	public static JdbcType driverJson(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.getJsonJdbcType( serviceRegistry );
	}

	/// Create the driver-backed PostgreSQL JSONB descriptor.
	public static JdbcType driverJsonb(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.getJsonbJdbcType( serviceRegistry );
	}

	/// Create the driver-backed PostgreSQL JSON-array type constructor.
	public static JdbcTypeConstructor driverJsonArrayConstructor(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.getJsonArrayJdbcType( serviceRegistry );
	}

	/// Create the driver-backed PostgreSQL JSONB-array type constructor.
	public static JdbcTypeConstructor driverJsonbArrayConstructor(ServiceRegistry serviceRegistry) {
		return PgJdbcHelper.getJsonbArrayJdbcType( serviceRegistry );
	}
}
