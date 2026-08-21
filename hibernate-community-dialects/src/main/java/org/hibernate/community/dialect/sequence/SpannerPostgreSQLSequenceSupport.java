/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.PostgreSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

public class SpannerPostgreSQLSequenceSupport extends PostgreSQLSequenceSupport {

	public static final SequenceSupport INSTANCE = new SpannerPostgreSQLSequenceSupport();

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
}
