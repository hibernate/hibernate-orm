package org.hibernate.dialect.temporaltype.spi;

import java.time.ZoneId;
import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// Immutable mapping inputs for a precision contribution. Sizes are copied as
/// scalar values, rather than exposing the mutable boot [org.hibernate.engine.jdbc.Size].
/// No connection, session or ambient timezone is part of this contract.
///
/// @param javaType the actual relational Java descriptor, after application conversion
/// @param jdbcType the actual executing JDBC descriptor, not merely its SQL code
/// @param dialect the configured Dialect, also available with boot metadata disabled
/// @param ddlTypeCode the resolved DDL type code
/// @param ddlTypeName the effective SQL column definition
/// @param declaredPrecision explicit secondPrecision, or null when omitted; zero is explicit
/// @param precision resolved column precision, or null for an unsized/nonfractional carrier
/// @param length resolved column length, when applicable
/// @param scale resolved column scale, when applicable
/// @param explicitColumnDefinition whether the SQL definition was supplied by the mapping
/// @param role contextual mapping/operation name for diagnostics
/// @param operation the operation whose execution is being assessed
/// @param policy the selected temporal-parts contract
/// @param representation inline versus separate-column timezone representation
/// @param adjustment explicitly requested adjustment target, or null
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
public record TemporalPrecisionContext(
		JavaType<?> javaType,
		JdbcType jdbcType,
		Dialect dialect,
		int ddlTypeCode,
		String ddlTypeName,
		Integer declaredPrecision,
		Integer precision,
		Long length,
		Integer scale,
		boolean explicitColumnDefinition,
		String role,
		Operation operation,
		TemporalPreservationPolicy.Type policy,
		TimeZoneRepresentation.Type representation,
		ZoneId adjustment) {
	public TemporalPrecisionContext {
		Objects.requireNonNull( javaType, "javaType" );
		Objects.requireNonNull( jdbcType, "jdbcType" );
		Objects.requireNonNull( dialect, "dialect" );
		Objects.requireNonNull( ddlTypeName, "ddlTypeName" );
		Objects.requireNonNull( role, "role" );
		Objects.requireNonNull( operation, "operation" );
		Objects.requireNonNull( policy, "policy" );
		Objects.requireNonNull( representation, "representation" );
	}

	/// Assignment preparation must not be reused to narrow a comparison operand.
	public enum Operation {
		ATTRIBUTE, VERSION, PERIOD, AGGREGATE, COMPARISON, CALLABLE, EXPRESSION
	}
}
