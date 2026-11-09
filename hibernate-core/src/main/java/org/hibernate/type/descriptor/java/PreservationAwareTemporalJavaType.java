package org.hibernate.type.descriptor.java;

import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Optional declaration of the independent temporal information required to
/// reconstruct a value. This does not certify its conversion through any JDBC type.
/// Existing Java Time domain contracts cannot be weakened by this declaration.
///
/// An implementation and its subclasses must keep this declaration consistent with
/// their semantics. Unrelated custom domains opt into preservation validation by
/// implementing this interface; the older [TemporalJavaType] contract is unchanged.
///
/// @param <T> the represented temporal value type
/// @since 8.0
/// @author Steve Ebersole
@Incubating(since = "8.0")
@SPI({ USE, IMPLEMENT })
public interface PreservationAwareTemporalJavaType<T> extends TemporalJavaType<T> {

	/// The nonempty, non-null requirement set, containing no null elements.
	/// For example, OffsetDateTime requires INSTANT and OFFSET, since those determine
	/// local date/time. The set must remain stable for this descriptor's lifetime.
	/// Hibernate defensively copies it and never modifies the supplied set.
	Set<TemporalPart> getRequiredTemporalParts();
}
