package org.hibernate.dialect.sequence.internal;

import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sequence.spi.NextvalSequenceSupport;

import jakarta.annotation.Nullable;

/**
 * Sequence support for {@link org.hibernate.dialect.HANADialect}.
 *
 * @author Gavin King
 */
public final class HANASequenceSupport extends NextvalSequenceSupport {

	private static final SequenceSupport INSTANCE = new HANASequenceSupport();

	public static SequenceSupport getInstance() {
		return INSTANCE;
	}

	@Override
	public String getFromDual() {
		return " from sys.dummy";
	}

	@Override
	public @Nullable String getFromGenerateSeries() {
		return " from series_generate_integer(1,1,?)";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}
}
