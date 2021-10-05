/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Types;

import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;

/**
 * A type that maps between {@link Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringType
		extends AbstractSingleColumnStandardBasicType<String>
		implements DiscriminatorType<String>, AdjustableBasicType<String> {

	public static final StringType INSTANCE = new StringType();

	public StringType() {
		super( VarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "string";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	public String stringToObject(CharSequence sequence) throws Exception {
		return sequence.toString();
	}

	public String toString(String value) {
		return value;
	}

}
