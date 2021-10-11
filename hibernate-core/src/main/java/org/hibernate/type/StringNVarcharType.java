/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NVarcharJdbcType;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringNVarcharType
		extends AbstractSingleColumnStandardBasicType<String> {

	public static final StringNVarcharType INSTANCE = new StringNVarcharType();

	public StringNVarcharType() {
		super( NVarcharJdbcType.INSTANCE, StringJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "nstring";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return false;
	}
}
