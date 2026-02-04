/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.community.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.sequence.PostgreSQLSequenceSupport;

public class SpannerPostgreSQLSequenceSupport extends PostgreSQLSequenceSupport {

	private final SpannerPostgreSQLDialect dialect;

	public SpannerPostgreSQLSequenceSupport(SpannerPostgreSQLDialect dialect) {
		super();
		this.dialect = dialect;
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		if ( incrementSize == 0 ) {
			throw new MappingException( "Unable to create the sequence [" + sequenceName + "]: the increment size must not be 0" );
		}
		return getCreateSequenceString( sequenceName )
			+ startingValue( initialValue, incrementSize )
			+ " start counter with " + initialValue;
	}

	@Override
	public String getRestartSequenceString(String sequenceName, long startWith) {
		return "alter sequence " + sequenceName + " restart counter with " + startWith;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		var nextValString = super.getSelectSequenceNextValString( sequenceName );
		if (dialect.useIntegerForPrimaryKey()) {
			nextValString = "spanner.bit_reverse(" + nextValString + ", true)";
		}
		return nextValString;
	}
}
