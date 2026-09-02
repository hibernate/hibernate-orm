/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence.spi;

import org.hibernate.MappingException;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Db2/Derby family base using ANSI value expressions, `values` stand-alone
/// selection, and restrictive sequence drop syntax.
///
/// Extend this class for a family member which retains those common rules and
/// override only its differing DDL or value-selection methods.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
@SPI({ USE, IMPLEMENT })
public class DB2SequenceSupport extends ANSISequenceSupport {
	/// Create a Db2-family strategy or provider subclass.
	@SPI(IMPLEMENT)
	public DB2SequenceSupport() {
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "values " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "previous value for " + sequenceName;
	}

	@Override
	public String getSequencePreviousValString(String sequenceName) {
		return "values " + getSelectSequencePreviousValString( sequenceName );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}
}
