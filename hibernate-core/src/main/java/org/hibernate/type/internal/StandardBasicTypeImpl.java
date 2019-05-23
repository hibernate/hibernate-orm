/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class StandardBasicTypeImpl<J> extends AbstractSingleColumnStandardBasicType {
	public static final String[] NO_REG_KEYS = new String[0];

	public StandardBasicTypeImpl(JavaTypeDescriptor<J> jtd, SqlTypeDescriptor std) {
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
