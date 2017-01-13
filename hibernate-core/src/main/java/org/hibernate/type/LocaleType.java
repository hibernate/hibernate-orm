/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Locale;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.LocaleJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and @link Locale}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LocaleType extends BasicTypeImpl<Locale> {

	public static final LocaleType INSTANCE = new LocaleType();

	public LocaleType() {
		super( LocaleJavaDescriptor.INSTANCE, VarcharSqlDescriptor.INSTANCE );
	}

	public String getName() {
		return "locale";
	}

	@Override
	public JdbcLiteralFormatter<Locale> getJdbcLiteralFormatter() {
		return VarcharSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( LocaleJavaDescriptor.INSTANCE );
	}
}
