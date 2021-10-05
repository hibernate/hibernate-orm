/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NVarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringNVarcharType
		extends AbstractSingleColumnStandardBasicType<String>
		implements DiscriminatorType<String> {

	public static final StringNVarcharType INSTANCE = new StringNVarcharType();

	public StringNVarcharType() {
		super( NVarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "nstring";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return false;
	}

	public String stringToObject(CharSequence sequence) throws Exception {
		return sequence.toString();
	}

	public String toString(String value) {
		return value;
	}
}
