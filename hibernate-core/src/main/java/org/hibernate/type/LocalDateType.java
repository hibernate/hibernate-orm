/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.internal.descriptor.DateTimeUtils;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.DateTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class LocalDateType
		extends BasicTypeImpl<LocalDate>
		implements JdbcLiteralFormatter<LocalDate> {

	/**
	 * Singleton access
	 */
	public static final LocalDateType INSTANCE = new LocalDateType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH );

	/**
	 * NOTE: protected access to allow for sub-classing
	 */
	@SuppressWarnings("WeakerAccess")
	protected LocalDateType() {
		super( LocalDateJavaDescriptor.INSTANCE, DateTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalDate.class.getSimpleName();
	}

	@Override
	public JdbcLiteralFormatter<LocalDate> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(LocalDate value, Dialect dialect) {
		return toJdbcLiteral( value );
	}

	@SuppressWarnings("WeakerAccess")
	public String toJdbcLiteral(LocalDate value) {
		return DateTimeUtils.formatAsJdbcLiteralDate( value );
	}
}
