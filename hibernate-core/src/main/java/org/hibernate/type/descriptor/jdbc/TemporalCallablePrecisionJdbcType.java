package org.hibernate.type.descriptor.jdbc;

import java.util.Optional;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.temporaltype.spi.TemporalCallablePrecisionContext;
import org.hibernate.dialect.temporaltype.spi.TemporalPrecisionSupport;

/// Optional precision contribution for an actual callable input or output route.
/// Attribute and prepared-statement support do not certify callable transport.
/// Assessment must be deterministic and must not acquire JDBC connections.
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public interface TemporalCallablePrecisionJdbcType extends JdbcType {
	/// Empty declines to stock assessment; present unsupported is authoritative.
	/// Never return null. The quantizer is a representability test: execution must
	/// not substitute a rounded input or repair an authoritative output with it.
	Optional<TemporalPrecisionSupport> getTemporalCallablePrecisionSupport(TemporalCallablePrecisionContext context);
}
