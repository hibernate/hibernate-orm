/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.TimesTenDialect}.
 *
 * @author Gavin King
 */

/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown
 * at http://oss.oracle.com/licenses/upl
 *
 *  - The Class now implements 'SequenceSupport'
 *  - Added a custom definition for 'supportsSequences()'
 *  - Added a custom definition for 'supportsPooledSequences()'
 *  - Added a custom definition for 'getSelectSequenceNextValString(String sequenceName)'
 *  - Added a custom definition for 'getSequenceNextValString(String sequenceName)'
 *  - Added a custom definition for 'getCreateSequenceString(String sequenceName)'
 *  - Added a custom definition for 'getDropSequenceString(String sequenceName)'
 *
 *  @Author: Carlos Blanco
 *
*/
public final class TimesTenSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new TimesTenSequenceSupport();



	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + sequenceName + ".nextval from sys.dual";
	}
  
	@Override
	public String getFromDual() {
		return " from sys.dual";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}
}
