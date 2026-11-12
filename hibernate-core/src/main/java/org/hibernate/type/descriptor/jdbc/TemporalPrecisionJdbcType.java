package org.hibernate.type.descriptor.jdbc;

import java.util.Optional;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Optional precision contract for an actual mapping and execution role.
/// This is independent of [TemporalJdbcType]'s temporal-component assessment.
/// Neither a SQL type code nor a component preservation report certifies precision.
///
/// Implementations must be deterministic and side-effect-free, without boot JDBC
/// access. A deliberately inherited contribution must remain correct when a subclass
/// changes execution. Delegation to another route is allowed; assessment cycles are not.
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@SPI({ USE, IMPLEMENT })
public interface TemporalPrecisionJdbcType extends JdbcType {
	/// Empty declines this context, allowing stock fallback. A present unsupported
	/// result is authoritative and must not be replaced by optimistic fallback.
	/// Never return null. The result must describe the actual relational Java type,
	/// physical type and operation, including deterministic read reconstruction.
	Optional<TemporalPrecisionSupport> getTemporalPrecisionSupport(TemporalPrecisionContext context);
}
