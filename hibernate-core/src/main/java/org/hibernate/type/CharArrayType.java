/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.PrimitiveCharacterArrayJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@code char[]}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharArrayType extends BasicTypeImpl<char[]> {
	public static final CharArrayType INSTANCE = new CharArrayType();

	public CharArrayType() {
		super( PrimitiveCharacterArrayJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "characters"; 
	}

	@Override
	public JdbcLiteralFormatter<char[]> getJdbcLiteralFormatter() {
		return VarcharTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( PrimitiveCharacterArrayJavaDescriptor.INSTANCE );
	}
}
