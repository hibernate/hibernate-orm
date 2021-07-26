/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

/**
 * Descriptor for {@link Types#LONGNVARCHAR LONGNVARCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class LongNVarcharTypeDescriptor extends NVarcharTypeDescriptor {
	public static final LongNVarcharTypeDescriptor INSTANCE = new LongNVarcharTypeDescriptor();

	public LongNVarcharTypeDescriptor() {
	}

	@Override
	public String toString() {
		return "LongNVarcharTypeDescriptor";
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.LONGNVARCHAR;
	}
}
