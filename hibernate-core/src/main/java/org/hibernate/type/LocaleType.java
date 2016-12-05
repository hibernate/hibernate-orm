/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.LocaleTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and @link Locale}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LocaleType extends BasicTypeImpl<Locale> implements JdbcLiteralFormatter<Locale> {

	public static final LocaleType INSTANCE = new LocaleType();

	public LocaleType() {
		super( LocaleTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "locale";
	}

	@Override
	public JdbcLiteralFormatter<Locale> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Locale value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
