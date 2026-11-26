package org.hibernate.temporal;

import java.time.Instant;

import org.hibernate.SharedSessionContract;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

public class InstantChangesets implements ChangesetIdentifierSupplier<Instant> {
	static final String NAME = "org.hibernate.temporal.InstantChangesets";

	private static final Instant FIRST = Instant.parse( "2026-01-01T00:00:00Z" );
	private long next;

	@Override
	public Instant generateIdentifier(SharedSessionContract session) {
		// A whole second survives storage at the database's timestamp precision.
		return FIRST.plusSeconds( next++ );
	}
}
