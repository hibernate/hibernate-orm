/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.TimeTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetTime>
		implements VersionType<OffsetTime>, LiteralType<OffsetTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetTimeType INSTANCE = new OffsetTimeType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "HH:mm:ss.S xxxxx", Locale.ENGLISH );

	public OffsetTimeType() {
		super( TimeTypeDescriptor.INSTANCE, OffsetTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(OffsetTime value, Dialect dialect) throws Exception {
		return "{t '" + FORMATTER.format( value ) + "'}";
	}

	@Override
	public OffsetTime seed(SessionImplementor session) {
		return OffsetTime.now();
	}

	@Override
	public OffsetTime next(OffsetTime current, SessionImplementor session) {
		return OffsetTime.now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<OffsetTime> getComparator() {
		return ComparableComparator.INSTANCE;
	}

	@Override
	public String getName() {
		return OffsetTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
