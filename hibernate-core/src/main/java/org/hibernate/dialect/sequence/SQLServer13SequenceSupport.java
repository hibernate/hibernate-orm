/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

public class SQLServer13SequenceSupport extends SQLServerSequenceSupport{
	public static final SequenceSupport INSTANCE = new SQLServer13SequenceSupport();

	@Override
	public String getDropSequenceString(String sequenceName) throws MappingException {
		return "drop sequence if exists " + sequenceName;
	}
}
