/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.NVarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringNVarcharType
		extends BasicTypeImpl<String> implements JdbcLiteralFormatter<String> {

	public static final StringNVarcharType INSTANCE = new StringNVarcharType();

	public StringNVarcharType() {
		super( StringTypeDescriptor.INSTANCE, NVarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "nstring";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(String value, Dialect dialect) {
		// some DBs support prefixing the literal with "n".  is that a standard?
		return StringType.INSTANCE.toJdbcLiteral( value, dialect );
	}

	public String toString(String value) {
		return value;
	}
}
