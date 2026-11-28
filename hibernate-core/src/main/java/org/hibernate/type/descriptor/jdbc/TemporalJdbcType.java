package org.hibernate.type.descriptor.jdbc;

import java.util.Optional;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.spi.TemporalPreservation;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// A [JdbcType] which states for itself what it preserves of a Java Time value, for
/// use with [temporal preservation][org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION].
///
/// With temporal preservation enabled, a Java Time value is accepted only where the
/// combination of its [JavaType] and its `JdbcType` is reported as preserving it. The
/// [Dialect][Dialect#getTemporalPreservation] is asked first, and knows only the descriptors
/// it was written for. A descriptor/ supplied by an application or a library is unknown to it,
/// and is asked through this contract instead. The author of the descriptor is the one who
/// knows what it does with the JDBC driver.
///
/// A report is a statement by its author that values survive being written through the binder
/// of this descriptor and read back through its extractor, including the conversions done
/// by the `JavaType`. Hibernate acts on it as given: the mapping is checked against it when
/// the factory is built, and every value bound or read is held to its range. A class of
/// Java values or a SQL type code alone establishes nothing, so a subclass which changes
/// how values are exchanged must revise the report it inherits.
///
/// Values are checked against the report as they pass through [BasicBinder]
/// and [BasicExtractor]. The binder and the extractor of the descriptor must
/// therefore extend those, be created for this descriptor and not borrowed
/// from another, and must not override `BasicExtractor.extract()`.
/// A mapping which uses a descriptor that does otherwise is rejected. A value
/// rendered as a SQL literal in a query is checked before the literal formatter
/// of the descriptor is asked, so the formatter may be of any kind.
///
/// @apiNote The Dialect is asked first. A Dialect knows only the descriptors
/// it was written for, and answers "unknown" for any other. This report is
/// used whenever the Dialect answers "unknown". If the Dialect does report
/// on this descriptor, its report takes precedence.
///
/// @see Dialect#getTemporalPreservation
///
/// @since 8.1
/// @author Steve Ebersole
@Incubating(since = "8.1")
@SPI({ USE, IMPLEMENT })
public interface TemporalJdbcType extends JdbcType {
	/// What this descriptor preserves of the values of the given Java type
	/// on the given Dialect.
	///
	/// An empty result declines to report, and leaves the combination
	/// unassessed, which leads to rejection. A report is used as it is,
	/// including the parts it states as unsupported or unknown.
	///
	/// The answer must depend on nothing but the arguments, and must not
	/// need a connection to the database. It is requested for every bound
	/// and extracted value, so it should be cheap.
	///
	/// @return the report, or empty to decline; never `null`
	Optional<TemporalPreservation> getTemporalPreservation(JavaType<?> javaType, Dialect dialect);
}
