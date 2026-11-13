package org.hibernate.dialect.temporaltype.spi;

import java.time.ZoneId;
import java.util.Objects;

import jakarta.persistence.ParameterMode;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.annotations.TemporalPreservationPolicy;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.BasicType;

/// One actual callable transport boundary, independent of table-column sizing.
/// No routine catalog metadata or attribute precision is implied by this context.
///
/// @param type effective scalar type, retaining its converter and executing descriptor
/// @param dialect configured Dialect/version, also available without boot JDBC access
/// @param jdbcTypeCode effective JDBC transport/registration code, not a column definition
/// @param mode parameter mode; scalar returns use OUT
/// @param boundary input and output are assessed independently for INOUT
/// @param valueAccess actual setter/extractor route, not merely the ORM parameter name
/// @param registrationAccess actual output registration route, or NONE
/// @param functionReturn whether this boundary is the scalar function return
/// @param role routine and parameter/return role for diagnostics, without values
/// @param policy effective component-preservation policy
/// @param adjustment explicit adjustment target, never inferred from ambient timezone
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public record TemporalCallablePrecisionContext(
		BasicType<?> type, Dialect dialect, int jdbcTypeCode, ParameterMode mode,
		Boundary boundary, Access valueAccess, Access registrationAccess,
		boolean functionReturn, String role, TemporalPreservationPolicy.Type policy, ZoneId adjustment) {
	public TemporalCallablePrecisionContext {
		Objects.requireNonNull( type );
		Objects.requireNonNull( dialect );
		Objects.requireNonNull( mode );
		Objects.requireNonNull( boundary );
		Objects.requireNonNull( valueAccess );
		Objects.requireNonNull( registrationAccess );
		Objects.requireNonNull( role );
		Objects.requireNonNull( policy );
		if ( mode == ParameterMode.REF_CURSOR || valueAccess == Access.NONE
				|| registrationAccess == Access.RESULT_SET
				|| boundary == Boundary.INPUT && (mode == ParameterMode.OUT || functionReturn
						|| valueAccess == Access.RESULT_SET || registrationAccess != Access.NONE)
				|| boundary == Boundary.OUTPUT && mode == ParameterMode.IN
				|| functionReturn && mode != ParameterMode.OUT
				|| valueAccess == Access.RESULT_SET && (!functionReturn || registrationAccess != Access.NONE)
				|| boundary == Boundary.OUTPUT && valueAccess != Access.RESULT_SET && registrationAccess == Access.NONE ) {
			throw new IllegalArgumentException( "Inconsistent callable precision boundary for " + role );
		}
	}

	public TemporalPrecisionContext.Operation operation() { return TemporalPrecisionContext.Operation.CALLABLE; }

	public enum Boundary { INPUT, OUTPUT }
	public enum Access { NONE, INDEXED, NAMED, RESULT_SET }
}
