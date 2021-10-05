/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.sql.Types;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.CastType;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class StandardBasicTypeImpl<J>
		extends AbstractSingleColumnStandardBasicType
		implements AdjustableBasicType {
	public static final String[] NO_REG_KEYS = ArrayHelper.EMPTY_STRING_ARRAY;

	public StandardBasicTypeImpl(JavaTypeDescriptor<J> jtd, JdbcTypeDescriptor std) {
		//noinspection unchecked
		super( std, jtd );
	}

	@Override
	public String[] getRegistrationKeys() {
		// irrelevant - these are created on-the-fly
		return NO_REG_KEYS;
	}

	@Override
	public String getName() {
		// again, irrelevant
		return null;
	}

	@Override
	public CastType getCastType() {
		if ( getJavaTypeDescriptor() == BooleanJavaTypeDescriptor.INSTANCE ) {
			switch ( getJdbcTypeCode() ) {
				case Types.BIT:
				case Types.SMALLINT:
				case Types.TINYINT:
				case Types.INTEGER:
					return CastType.INTEGER_BOOLEAN;
				case Types.CHAR:
					return CastType.YN_BOOLEAN;
			}
		}
		return super.getCastType();
	}
}
