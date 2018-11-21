/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * {@link BasicType} impl support
 *
 * @author Steve Ebersole
 *
 * @deprecated Ported simply to support {@link BasicType} as closely as
 * possible to its legacy definition.  Users were often directed to use this
 * as a base for custom "types"
 *
 * @see BasicType
 */
@Deprecated
public abstract class AbstractSingleColumnStandardBasicType<T>
		extends AbstractStandardBasicType<T> {

	public AbstractSingleColumnStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		super( (org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor) sqlTypeDescriptor, javaTypeDescriptor );
	}

	public final int sqlType() {
		return getSqlTypeDescriptor().getSqlType();
	}
}
