/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringType extends BasicTypeImpl<String> {
	public static final StringType INSTANCE = new StringType();

	public StringType() {
		super( StringJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "string";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		return VarcharTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( StringJavaDescriptor.INSTANCE );
	}
}
