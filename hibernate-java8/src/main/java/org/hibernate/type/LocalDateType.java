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
import org.hibernate.type.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.sql.DateTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class LocalDateType
		extends AbstractSingleColumnStandardBasicType<LocalDate>
		implements LiteralType<LocalDate> {

	/**
	 * Singleton access
	 */
	public static final LocalDateType INSTANCE = new LocalDateType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd", Locale.ENGLISH );

	public LocalDateType() {
		super( DateTypeDescriptor.INSTANCE, LocalDateJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalDate.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(LocalDate value, Dialect dialect) throws Exception {
		return "{d '" + FORMATTER.format( value ) + "'}";
	}
}
