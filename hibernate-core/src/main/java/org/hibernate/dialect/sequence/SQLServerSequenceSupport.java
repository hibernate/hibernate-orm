/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.SQLServerDialect}.
 *
 * @author Christian Beikov
 */
public final class SQLServerSequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new SQLServerSequenceSupport();

	@Override
	public String getSequencePreviousValString(String sequenceName) throws MappingException {
		return "select convert(varchar(200),current_value) from sys.sequences where name='" + sequenceName + "'";
	}
}
