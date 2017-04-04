/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class LocalDateTimeType
		extends AbstractSingleColumnStandardBasicType<LocalDateTime>
		implements VersionType<LocalDateTime>, LiteralType<LocalDateTime> {
	/**
	 * Singleton access
	 */
	public static final LocalDateTimeType INSTANCE = new LocalDateTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S", Locale.ENGLISH );

	public LocalDateTimeType() {
		super( TimestampTypeDescriptor.INSTANCE, LocalDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(LocalDateTime value, Dialect dialect) throws Exception {
		return "{ts '" + FORMATTER.format( value ) + "'}";
	}

	@Override
	public LocalDateTime seed(SharedSessionContractImplementor session) {
		return LocalDateTime.now();
	}

	@Override
	public LocalDateTime next(LocalDateTime current, SharedSessionContractImplementor session) {
		return LocalDateTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<LocalDateTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}
}
