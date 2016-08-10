/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringType extends AbstractSingleColumnStandardBasicType<String> implements JdbcLiteralFormatter<String> {
	public static final StringType INSTANCE = new StringType();

	public StringType() {
		super( VarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "string";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(String value, Dialect dialect) {
		return value == null ? "NULL" : dialect.quote( value );
	}
}
