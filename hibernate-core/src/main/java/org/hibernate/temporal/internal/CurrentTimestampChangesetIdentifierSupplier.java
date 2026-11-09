package org.hibernate.temporal.internal;

import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

import java.time.Instant;

/**
 * Simple implementation that queries the database current timestamp.
 */
public class CurrentTimestampChangesetIdentifierSupplier implements ChangesetIdentifierSupplier<Instant> {
	@Override
	public Instant generateIdentifier(SharedSessionContract session) {
		final var implementor = (org.hibernate.engine.spi.SharedSessionContractImplementor) session;
		final var factory = implementor.getFactory();
		if ( factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ) {
			final var jdbcType = factory.getJdbcServices().getDialect().getTemporalTableSupport()
					.getPreservingTemporalJdbcType( factory.getSessionFactoryOptions().getTemporalTableStrategy(),
							factory.getTypeConfiguration().getJdbcTypeRegistry() );
			if ( jdbcType != null ) {
				return org.hibernate.generator.internal.TypedCurrentTimestamp.generate(
						implementor, org.hibernate.type.descriptor.java.InstantJavaType.INSTANCE, jdbcType );
			}
		}
		return session.createSelectionQuery( "select instant", Instant.class ).getSingleResult();
	}
}
