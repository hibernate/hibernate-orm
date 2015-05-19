/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.LocalTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimeTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.time.LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class LocalTimeType
		extends AbstractSingleColumnStandardBasicType<LocalTime>
		implements VersionType<LocalTime>, LiteralType<LocalTime> {
	/**
	 * Singleton access
	 */
	public static final LocalTimeType INSTANCE = new LocalTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "HH:mm:ss", Locale.ENGLISH );

	public LocalTimeType() {
		super( TimeTypeDescriptor.INSTANCE, LocalTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(LocalTime value, Dialect dialect) throws Exception {
		return "{t '" + FORMATTER.format( value ) + "'}";
	}

	@Override
	public LocalTime seed(SessionImplementor session) {
		return LocalTime.now();
	}

	@Override
	public LocalTime next(LocalTime current, SessionImplementor session) {
		return LocalTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<LocalTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}
}
