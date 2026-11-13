package org.hibernate.type.descriptor.jdbc;

import java.util.Optional;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Optional preservation assessment for this descriptor and an actual Java type
/// descriptor. Assess wrapping/unwrapping as well as JDBC binding and extraction;
/// a domain class or SQL code alone does not establish conversion compatibility.
///
/// Built-in dialects consult this contract before their existing pair assessments.
/// Application Dialect overrides remain authoritative unless they delegate.
/// Implementations may inherit a deliberately supported family contract, but a
/// subclass changing execution must update or decline the inherited assessment.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating(since = "8.0")
@SPI({ USE, IMPLEMENT })
public interface TemporalJdbcType extends JdbcType {

	/// An empty Optional declines this pair, permitting ordinary Dialect fallback.
	/// A present report is authoritative, including UNKNOWN or UNSUPPORTED parts;
	/// it is not merged with more optimistic fallback facts. Never return null.
	///
	/// The assessment must be deterministic and side-effect-free. It must not acquire
	/// connections or require boot metadata. It describes execution without configuring
	/// it, and does not certify arbitrary schemas, values or callable operations.
	/// Delegation to a distinct known pair is allowed; recursive pair assessment is not.
	Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect);
}
