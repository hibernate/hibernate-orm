/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

public class SQLServer16SequenceSupport extends SQLServerSequenceSupport{
	public static final SequenceSupport INSTANCE = new SQLServer16SequenceSupport();

	@Override
	public String getDropSequenceString(String sequenceName) throws MappingException {
		return "drop sequence if exists " + sequenceName;
	}
}
