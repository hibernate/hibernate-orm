/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.sql.Types;

/**
 * Descriptor for {@link Types#FLOAT FLOAT} handling.
 *
 * @author Steve Ebersole
 */
public class FloatTypeDescriptor extends RealTypeDescriptor {
	public static final FloatTypeDescriptor INSTANCE = new FloatTypeDescriptor();

	public FloatTypeDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.FLOAT;
	}
}
