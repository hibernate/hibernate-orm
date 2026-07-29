package org.hibernate.dialect.sequence.internal;

import jakarta.annotation.Nullable;
import org.hibernate.dialect.sequence.spi.SequenceSupport;

import org.hibernate.MappingException;

public class SQLServer16SequenceSupport extends SQLServerSequenceSupport{
	private static final SequenceSupport INSTANCE = new SQLServer16SequenceSupport();

	public static SequenceSupport getInstance() {
		return INSTANCE;
	}

	@Override
	public String getDropSequenceString(String sequenceName) throws MappingException {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public @Nullable String getFromGenerateSeries() {
		return " from generate_series(1,?)";
	}
}
