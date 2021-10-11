/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicTypeImpl<J> extends AbstractSingleColumnStandardBasicType<J> implements AdjustableBasicType<J> {
	public static final String[] NO_REG_KEYS = ArrayHelper.EMPTY_STRING_ARRAY;

	public BasicTypeImpl(JavaType<J> jtd, JdbcTypeDescriptor std) {
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
}
