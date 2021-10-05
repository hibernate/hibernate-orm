/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

/**
 * Descriptor for {@link Types#NUMERIC NUMERIC} handling.
 *
 * @author Steve Ebersole
 */
public class NumericJdbcTypeDescriptor extends DecimalJdbcTypeDescriptor {
	public static final NumericJdbcTypeDescriptor INSTANCE = new NumericJdbcTypeDescriptor();

	public NumericJdbcTypeDescriptor() {
	}

	@Override
	public String toString() {
		return "NumericTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.NUMERIC;
	}
}
